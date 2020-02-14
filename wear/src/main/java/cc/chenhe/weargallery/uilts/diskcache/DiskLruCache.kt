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

package cc.chenhe.weargallery.uilts.diskcache

import cc.chenhe.weargallery.uilts.loge
import okio.BufferedSink
import okio.BufferedSource
import okio.Okio
import java.io.File
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

private const val TAG = "DiskLruCache"

private const val JOURNAL_MAX_COUNT = 1000

private const val JOURNAL_FILE = "journal"
private const val JOURNAL_FILE_TEMP = "$JOURNAL_FILE.tmp"
private const val JOURNAL_FILE_BACKUP = "$JOURNAL_FILE.bak"
private const val MAGIC = "cc.chenhe.DiskLruCache"
private const val VERSION_1 = "1"

private const val CLEAN = "CLEAN"
private const val DIRTY = "DIRTY"
private const val REMOVE = "REMOVE"
private const val READ = "READ"

/**
 * We won't write [READ] log if this flag is `false` since this type of log is useless.
 */
private const val DEBUG = false

/**
 * Based on [DiskLruCache](https://github.com/JakeWharton/DiskLruCache), adapted to our use case.
 */
class DiskLruCache private constructor(
        private val directory: File,
        private val appVersion: Long,
        private val maxSize: Long
) {

    private val journalFile = File(directory, JOURNAL_FILE)
    private val journalFileTmp = File(directory, JOURNAL_FILE_TEMP)
    private val journalFileBackup = File(directory, JOURNAL_FILE_BACKUP)

    private val lruEntries = LinkedHashMap<String, Entry>(0, 0.75f, true)
    private var redundantOpCount = 0
    private var size: Long = 0

    private val executorService = ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, LinkedBlockingQueue())
    private val cleanupCallable = Callable {
        synchronized(this) {
            trimToSize()
            if (journalRebuildRequired()) {
                rebuildJournal()
                redundantOpCount = 0
            }
        }
    }


    companion object {

        private val legalPattern = Pattern.compile("[a-z0-9_-]{1,64}")

        fun open(directory: File, appVersion: Long, maxSize: Long): DiskLruCache {
            if (maxSize <= 0) {
                throw IllegalArgumentException("maxSize must > 0")
            }

            // Check backup file
            val bak = File(directory, JOURNAL_FILE_BACKUP)
            if (bak.exists()) {
                val journal = File(directory, JOURNAL_FILE)
                if (journal.exists()) {
                    // If journal file also exists just delete backup file.
                    bak.delete()
                } else {
                    bak.renameTo(journal, false)
                }
            }

            val cache = DiskLruCache(directory, appVersion, maxSize)
            if (cache.journalFile.exists()) {
                try {
                    cache.readJournal()
                    cache.processJournal()
                    return cache
                } catch (e: Exception) {
                    // Rebuild the cache if any errors are encountered
                    loge(TAG, "DiskLruCache $directory is corrupt: ${e.message}, removing")
                    cache.delete()
                }
            }

            // Create a new view_empty cache
            directory.mkdirs()
            cache.rebuildJournal()
            return cache
        }
    }

    /**
     * Creates a new journal that omits redundant information. This replaces the current journal if it exists.
     *
     * @throws IOException
     */
    @Synchronized
    private fun rebuildJournal() {
        journalFileTmp.appendingSink { bufferedSink ->
            // header
            bufferedSink.apply {
                writeUtf8(MAGIC)
                writeUtf8("\n")
                writeUtf8(VERSION_1)
                writeUtf8("\n")
                writeUtf8(appVersion.toString())
                writeUtf8("\n\n")
            }

            // entries
            lruEntries.values.forEach {
                if (it.currentEditor != null) {
                    bufferedSink.writeDirty(it)
                } else {
                    bufferedSink.writeClean(it)
                }
            }
        }

        // write to disk
        if (journalFile.exists()) {
            journalFile.renameTo(journalFileBackup, true)
        }
        journalFileTmp.renameTo(journalFile, false)
        journalFileBackup.delete()
    }

    /**
     * **Should only be called by [open] to instantiate.**
     *
     * @throws IOException If the journal verification fails or the app version does not match.
     */
    @Throws(IOException::class)
    private fun readJournal() {
        journalFile.source { bufferedSource ->
            val magic = bufferedSource.readUtf8Line()
            val version = bufferedSource.readUtf8Line()
            val appVersionStr = bufferedSource.readUtf8Line()
            val blank = bufferedSource.readUtf8Line()
            if (magic != MAGIC || version != VERSION_1 || blank != "") {
                throw IOException("Unexpected journal header: [$magic, $version, $blank]")
            }
            if (appVersion.toString() != appVersionStr) {
                throw IOException("Unexpected journal header: app version mismatch.")
            }

            var lineCount = 0
            while (true) {
                try {
                    val line = bufferedSource.readUtf8Line() ?: break
                    lineCount++
                    readJournalLine(line)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            redundantOpCount = lineCount - lruEntries.size
        }
    }

    /**
     * **Should only be called by [readJournal] to instantiate.**
     *
     * @throws IOException If the entry format is malformed.
     */
    @Throws(IOException::class)
    private fun readJournalLine(line: String) {
        val firstSpace = line.indexOf(' ')
        if (firstSpace == -1) {
            throw IOException("Unexpected journal line: $line")
        }

        val keyBegin = firstSpace + 1
        val secondSpace = line.indexOf(' ', keyBegin)
        val key: String
        if (secondSpace == -1) {
            key = line.substring(keyBegin)
            if (firstSpace == REMOVE.length && line.startsWith(REMOVE)) {
                // REMOVE
                lruEntries.remove(key)
                return
            }
        } else {
            key = line.substring(keyBegin, secondSpace)
        }

        var entry = lruEntries[key]
        if (entry == null) {
            entry = Entry(key).also { lruEntries[key] = it }
        }

        if (secondSpace != -1 && firstSpace == CLEAN.length && line.startsWith(CLEAN)) {
            // CLEAN
            entry.reset(
                    readable = true,
                    currentEditor = null,
                    length = line.substring(secondSpace + 1).toLong()
            )
        } else if (secondSpace == -1 && firstSpace == DIRTY.length && line.startsWith(DIRTY)) {
            // DIRTY
            entry.currentEditor = EditorImpl(entry)
            // We don't need to set other properties
            // because without a matching terminator, it is considered an invalid entry.
        } else if (secondSpace == -1 && firstSpace == READ.length && line.startsWith(READ)) {
            // READ
            // nothing to do
        } else {
            throw IOException("Unexpected journal line: $line")
        }
    }

    /**
     * Computes the initial size and collects garbage as a part of opening the cache. Dirty entries are assumed to be
     * inconsistent and will be deleted.
     *
     * **Should only be called by [open] to instantiate.**
     */
    private fun processJournal() {
        journalFileTmp.deleteSure()

        size = 0
        val iterator = lruEntries.values.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.currentEditor == null) {
                size += entry.length
            } else {
                // This entry dose not have a matching terminator. Let's consider it as an invalid entry and delete it.
                entry.reset(false, null, 0)
                entry.cleanFile.deleteSure()
                entry.dirtyFile.deleteSure()
                iterator.remove()
            }
        }
    }

    private fun trimToSize() {
        while (size > maxSize) {
            val toEvict = lruEntries.entries.iterator().next()
            remove(toEvict.key)
        }
    }

    private fun delete() {
        directory.deleteRecursivelySure()
    }

    private fun validateKey(key: String) {
        val matcher = legalPattern.matcher(key)
        require(matcher.matches()) { "Keys must match regex [a-z0-9_-]{1,64}: [$key]" }
    }

    @Synchronized
    private fun completeEdit(editor: EditorImpl, success: Boolean) {
        val entry = editor.entry
        if (entry.currentEditor != editor) {
            throw IllegalStateException()
        }

        val dirty = entry.dirtyFile
        if (success && !dirty.exists()) {
            editor.abort()
            return
        }

        if (success) {
            val clean = entry.cleanFile
            dirty.renameTo(clean, true)
            val oldLength = entry.length
            val newLength = clean.length()
            entry.length = newLength
            size = size - oldLength + newLength
        } else {
            dirty.deleteSure()
        }

        redundantOpCount++
        entry.currentEditor = null
        if (entry.readable || success) {
            entry.readable = true
            journalFile.appendingSink { it.writeClean(entry) }
        } else {
            lruEntries.remove(entry.key)
            journalFile.appendingSink { it.writeRemove(entry) }
        }

        if (size > maxSize || journalRebuildRequired()) {
            executorService.submit(cleanupCallable)
        }
    }

    /**
     * We only rebuild the journal when it will halve the size of the journal and eliminate at least
     * [JOURNAL_MAX_COUNT] ops.
     */
    private fun journalRebuildRequired(): Boolean {
        return (redundantOpCount >= JOURNAL_MAX_COUNT && redundantOpCount >= lruEntries.size)
    }


    // ---------------------------------------------------------------------------
    // API
    // ---------------------------------------------------------------------------

    /**
     * Returns an editor for the entry named [key], or null if another edit is in progress.
     *
     * @throws IOException
     */
    @Synchronized
    fun edit(key: String): Editor? {
        validateKey(key)
        val entry = lruEntries[key] ?: Entry(key).also { lruEntries[key] = it }
        if (entry.currentEditor != null) {
            // Another edit is in progress.
            return null
        }

        val editor = EditorImpl(entry)
        entry.currentEditor = editor

        journalFile.appendingSink { it.writeDirty(entry) }
        return editor
    }

    /**
     * Drops the entry for [key] if it exists and can be removed. Entries actively being edited cannot be removed.
     *
     * @return true if an entry was removed.
     */
    @Synchronized
    fun remove(key: String): Boolean {
        validateKey(key)
        val entry = lruEntries[key]
        if (entry == null || entry.currentEditor != null) {
            return false
        }

        entry.cleanFile.deleteSure()
        size -= entry.length
        entry.reset(false, null, 0)

        redundantOpCount++
        journalFile.appendingSink { it.writeRemove(entry) }
        lruEntries.remove(key)

        if (journalRebuildRequired()) {
            executorService.submit(cleanupCallable)
        }
        return true
    }

    /**
     * Get a cached file for the entry named [key].
     *
     * **Warning**: There is no guarantee of the immutability of the returned file.You shouldn't consider it as a
     * snapshot.
     */
    @Synchronized
    fun getFile(key: String): File? {
        validateKey(key)
        val entry = lruEntries[key] ?: return null
        if (!entry.readable) {
            return null
        }

        if (!entry.cleanFile.exists()) {
            // A file must have been deleted manually!
            remove(key)
            return null
        }

        if (DEBUG) {
            redundantOpCount++
            journalFile.appendingSink { it.writeRead(entry) }
        }
        if (journalRebuildRequired()) {
            executorService.submit(cleanupCallable)
        }
        return entry.cleanFile
    }

    // ---------------------------------------------------------------------------
    // Class
    // ---------------------------------------------------------------------------

    interface Editor {

        /**
         * Get a file that caller should write into.
         */
        fun getTargetFile(): File

        fun commit()

        fun abort()

    }

    private inner class EditorImpl(val entry: Entry) : Editor {

        override fun getTargetFile(): File {
            synchronized(this@DiskLruCache) {
                if (entry.currentEditor != this) {
                    throw IllegalStateException()
                }
                return entry.dirtyFile
            }
        }

        override fun commit() {
            completeEdit(this, true)
        }

        override fun abort() {
            completeEdit(this, false)
        }

    }

    private inner class Entry(val key: String) {
        /** Length of this entry's file. */
        var length: Long = 0

        /** True if this entry has ever been published. */
        var readable: Boolean = false

        /** The ongoing edit or null if this entry is not being edited. */
        var currentEditor: EditorImpl? = null

        val cleanFile get() = File(directory, key)

        val dirtyFile get() = File(directory, "$key.tmp")

        /**
         * Reset all member variables to avoid omissions.
         */
        fun reset(readable: Boolean, currentEditor: EditorImpl?, length: Long) {
            this.readable = readable
            this.currentEditor = currentEditor
            this.length = length
        }
    }

    // ---------------------------------------------------------------------------
    // Util
    // ---------------------------------------------------------------------------

    private fun BufferedSink.writeDirty(entry: Entry) {
        writeUtf8(DIRTY)
        writeUtf8(" ")
        writeUtf8(entry.key)
        writeUtf8("\n")
    }

    private fun BufferedSink.writeClean(entry: Entry) {
        writeUtf8(CLEAN)
        writeUtf8(" ")
        writeUtf8(entry.key)
        writeUtf8(" ")
        writeUtf8(entry.length.toString())
        writeUtf8("\n")
    }

    private fun BufferedSink.writeRemove(entry: Entry) {
        writeUtf8(REMOVE)
        writeUtf8(" ")
        writeUtf8(entry.key)
        writeUtf8("\n")
    }

    private fun BufferedSink.writeRead(entry: Entry) {
        writeUtf8(READ)
        writeUtf8(" ")
        writeUtf8(entry.key)
        writeUtf8("\n")
    }

}


// ---------------------------------------------------------------------------
// Util
// ---------------------------------------------------------------------------

/**
 * Make sure rename a file and throw an exception if it fails.
 *
 * @throws IOException
 */
private fun File.renameTo(target: File, override: Boolean) {
    if (override) {
        target.deleteSure()
    }
    if (!renameTo(target)) {
        throw IOException()
    }
}

/**
 * Make sure delete a file if it exists and throw an exception if it fails.
 *
 * @throws IOException
 */
private fun File.deleteSure() {
    if (exists() && !delete()) {
        throw IOException("Delete file error")
    }
}

/**
 * Make sure delete a folder if it exists and throw an exception if it fails.
 *
 * @throws IOException
 */
private fun File.deleteRecursivelySure() {
    if (exists() && !deleteRecursively()) {
        throw IOException("Delete file error")
    }
}

/**
 * Create a [BufferedSink] that will be auto closed to write file.
 *
 * @param autoCreate Create a new file first if it doesn't exist.
 * @throws IOException
 */
private inline fun File.appendingSink(autoCreate: Boolean = true, block: (bufferedSink: BufferedSink) -> Unit) {
    if (autoCreate && !this.exists()) {
        this.createNewFile()
    }
    Okio.appendingSink(this).use {
        Okio.buffer(it).use { bufferedSink ->
            block(bufferedSink)
        }
    }
}

/**
 * Create a [BufferedSource] that will be auto closed to read file.
 *
 * @throws IOException
 */
private fun File.source(block: (bufferedSource: BufferedSource) -> Unit) {
    Okio.source(this).use {
        Okio.buffer(it).use { bufferedSource ->
            block(bufferedSource)
        }
    }
}
