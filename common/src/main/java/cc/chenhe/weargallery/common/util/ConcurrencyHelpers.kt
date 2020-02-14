/**
 * Copyright (C) 2020 Chenhe
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cc.chenhe.weargallery.common.util

import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineStart.LAZY
import java.util.concurrent.ConcurrentHashMap
import kotlin.DeprecationLevel.ERROR


/**
 * A controlled runner decides what to do when new tasks are run.
 *
 * Note: This implementation is for example only. It will not work in the presence of
 *       multi-threading and is not safe to call from Dispatchers.IO or Dispatchers.Default. In
 *       real code use the thread-safe implementation of [ControlledRunner] code listed below.
 *
 * By calling [joinPreviousOrRun], the new task will be discarded and the result of the previous task
 * will be returned. This is useful when you want to ensure that a network request to the same
 * resource does not flood.
 *
 * By calling [cancelPreviousThenRun], the old task will *always* be cancelled and then the new task will
 * be run. This is useful in situations where a new event implies that the previous work is no
 * longer relevant such as sorting or filtering a list.
 */
@Deprecated("This code is not thread-safe and should not be used. Use " +
        "the ControlledRunner implementation below instead.", level = ERROR)
class ControlledRunnerExampleImplementation<T> {
    private var activeTask: Deferred<T>? = null

    /**
     * Cancel all previous tasks before calling block.
     *
     * When several coroutines call cancelPreviousThenRun at the same time, only one will run and
     * the others will be cancelled.
     */
    @Deprecated("This code is not thread-safe. Use ControlledRunner below instead.",
            level = ERROR)
    suspend fun cancelPreviousThenRun(block: suspend () -> T): T {
        // If there is an activeTask, cancel it because it's result is no longer needed
        //
        // By waiting for the cancellation to complete with `cancelAndJoin` we know that activeTask
        // has stopped executing before continuing.
        activeTask?.cancelAndJoin()

        // use a coroutineScope builder to safely start a new coroutine in a suspend function
        return coroutineScope {
            // create a new task to call the block
            val newTask = async {
                block()
            }
            // when the new task completes, reset activeTask to null
            // this will be called by cancellation as well as normal completion
            newTask.invokeOnCompletion {
                activeTask = null
            }
            // save the newTask to activeTask, then wait for it to complete and return the result
            activeTask = newTask
            newTask.await()
        }
    }

    /**
     * Don't run the new block if a previous block is running, instead wait for the previous block
     * and return it's result.
     *
     * When several coroutines call joinPreviousOrRun at the same time, only one will run and
     * the others will return the result from the winner.
     */
    @Deprecated("This code is not thread-safe. Use ControlledRunner below instead.",
            level = ERROR)
    suspend fun joinPreviousOrRun(block: suspend () -> T): T {
        // if there is an activeTask, return it's result and don't run the block
        activeTask?.let {
            return it.await()
        }

        // use a coroutineScope builder to safely start a new coroutine in a suspend function
        return coroutineScope {
            // create a new task to call the block
            val newTask = async {
                block()
            }
            // when the task completes, reset activeTask to null
            newTask.invokeOnCompletion {
                activeTask = null
            }
            // save newTask to activeTask, then wait for it to complete and return the result
            activeTask = newTask
            newTask.await()
        }
    }
}

private val KEY_DEFAULT = "default"

/**
 * A controlled runner decides what to do when new tasks are run.
 *
 * By calling [joinPreviousOrRun], the new task will be discarded and the result of the previous task will be returned.
 * This is useful when you want to ensure that a network request to the same resource does not flood.
 *
 * By calling [cancelPreviousThenRun], the old task will *always* be cancelled and then the new task will be run. This
 * is useful in situations where a new event implies that the previous work is no longer relevant such as sorting or
 * filtering a list.
 */
class ControlledRunner<T> {
    /**
     * The currently active tasks.
     *
     * This uses an [ConcurrentHashMap] to ensure that it's safe to update [activeTasks] on both [Dispatchers.Default]
     * and [Dispatchers.Main] which will execute coroutines on multiple threads at the same time.
     */
    private val activeTasks = ConcurrentHashMap<String, Deferred<T>>()

    /**
     * Cancel all previous tasks before calling block.
     *
     * When several coroutines call cancelPreviousThenRun at the same time, only one will run and the others will be
     * cancelled.
     *
     * In the following example, only one sort operation will execute and any previous sorts will be cancelled.
     *
     * ```
     * class Products {
     *    val controlledRunner = ControlledRunner<Product>()
     *
     *    fun sortAscending(): List<Product> {
     *        return controlledRunner.cancelPreviousThenRun { dao.loadSortedAscending() }
     *    }
     *
     *    fun sortDescending(): List<Product> {
     *        return controlledRunner.cancelPreviousThenRun { dao.loadSortedDescending() }
     *    }
     * }
     * ```
     *
     * @param block The code to run after previous work is cancelled.
     * @return The result of block, if this call was not cancelled prior to returning.
     */
    suspend fun cancelPreviousThenRun(key: String = KEY_DEFAULT, block: suspend () -> T): T {
        // fast path: if we already know about an active task, just cancel it right away.
        activeTasks[key]?.cancelAndJoin()

        return coroutineScope {
            // Create a new coroutine, but don't start it until it's decided that this block should
            // execute. In the code below, calling await() on newTask will cause this coroutine to
            // start.
            val newTask = async(start = LAZY) {
                block()
            }

            // When newTask completes, ensure that it resets activeTask to null (if it was the
            // current activeTask).
            newTask.invokeOnCompletion {
                activeTasks.remove(key, newTask)
            }

            // Kotlin ensures that we only set result once since it's a val, even though it's set
            // inside the while(true) loop.
            val result: T

            // Loop until we are sure that newTask is ready to execute (all previous tasks are
            // cancelled)
            while (true) {
                if (activeTasks.putIfAbsent(key, newTask) != null) {
                    // some other task started before newTask got set to activeTask, so see if it's
                    // still running when we call get() here. If so, we can cancel it.

                    // we will always start the loop again to see if we can set activeTask before
                    // starting newTask.
                    activeTasks[key]?.cancelAndJoin()
                    // yield here to avoid a possible tight loop on a single threaded dispatcher
                    yield()
                } else {
                    // happy path - we set activeTask so we are ready to run newTask
                    result = newTask.await()
                    break
                }
            }

            // Kotlin ensures that the above loop always sets result exactly once, so we can return
            // it here!
            result
        }
    }

    /**
     * Don't run the new block if a previous block is running, instead wait for the previous block and return it's
     * result.
     *
     * When several coroutines call jonPreviousOrRun at the same time, only one will run and the others will return the
     * result from the winner.
     *
     * In the following example, only one network operation will execute at a time and any other requests will return
     * the result from the "in flight" request.
     *
     * ```
     * class Products {
     *    val controlledRunner = ControlledRunner<Product>()
     *
     *    fun fetchProducts(): List<Product> {
     *        return controlledRunner.joinPreviousOrRun {
     *            val results = api.fetchProducts()
     *            dao.insert(results)
     *            results
     *        }
     *    }
     * }
     * ```
     *
     * @param block The code to run if and only if no other task is currently running
     * @return The result of block, or if another task was running the result of that task instead.
     */
    suspend fun joinPreviousOrRun(key: String = KEY_DEFAULT, block: suspend () -> T): T {
        // fast path: if there's already an active task, just wait for it and return the result
        activeTasks[key]?.let {
            return it.await()
        }
        return coroutineScope {
            // Create a new coroutine, but don't start it until it's decided that this block should
            // execute. In the code below, calling await() on newTask will cause this coroutine to
            // start.
            val newTask = async(start = LAZY) {
                block()
            }

            newTask.invokeOnCompletion {
                activeTasks.remove(key, newTask)
            }

            // Kotlin ensures that we only set result once since it's a val, even though it's set
            // inside the while(true) loop.
            val result: T

            // Loop until we figure out if we need to run newTask, or if there is a task that's
            // already running we can join.
            while (true) {
                if (activeTasks.putIfAbsent(key, newTask) != null) {
                    // some other task started before newTask got set to activeTask, so see if it's
                    // still running when we call get() here. There is a chance that it's already
                    // been completed before the call to get, in which case we need to start the
                    // loop over and try again.
                    val currentTask = activeTasks[key]
                    if (currentTask != null) {
                        // happy path - we found the other task so use that one instead of newTask
                        newTask.cancel()
                        result = currentTask.await()
                        break
                    } else {
                        // retry path - the other task completed before we could get it, loop to try
                        // setting activeTask again.

                        // call yield here in case we're executing on a single threaded dispatcher
                        // like Dispatchers.Main to allow other work to happen.
                        yield()
                    }
                } else {
                    // happy path - we were able to set activeTask, so start newTask and return its
                    // result
                    result = newTask.await()
                    break
                }
            }

            // Kotlin ensures that the above loop always sets result exactly once, so we can return
            // it here!
            result
        }
    }
}