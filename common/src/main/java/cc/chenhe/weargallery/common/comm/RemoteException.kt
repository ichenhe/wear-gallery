package cc.chenhe.weargallery.common.comm

import me.chenhe.lib.wearmsger.bean.BothWayCallback
import me.chenhe.lib.wearmsger.bean.MessageCallback
import java.nio.charset.Charset

/**
 * Represent a failed communication.
 */
class RemoteException(val type: Type, cause: Throwable? = null) :
    RuntimeException("Failed to get response, cause=${type.name}.", cause) {

    enum class Type {
        TIMEOUT,
        EMPTY,
        REQUEST_FAIL,
    }
}

/**
 * Check if a callback is success. If not a [RemoteException] will be thrown, else noting happen.
 *
 * @param hasData If true, the callback must have data, or a exception will be thrown.
 * @param dataIsString Require the data is a valid utf8 string. It only makes sense when [hasData] is true.
 * @return A decoded string with utf8 when [dataIsString] is true, else is null.
 * @throws RemoteException If the check is failed.
 */
fun MessageCallback.check(hasData: Boolean, dataIsString: Boolean = true): String? {
    if (isSuccess()) {
        if (hasData) {
            if (data == null)
                throw RemoteException(RemoteException.Type.EMPTY)
            if (dataIsString) {
                try {
                    return String(data!!, Charset.forName("utf-8"))
                } catch (e: Exception) {
                    throw RemoteException(RemoteException.Type.EMPTY, e)
                }
            }
        }
        return null
    }
    when (result) {
        BothWayCallback.Result.OK -> throw IllegalStateException() // should never happen
        BothWayCallback.Result.REQUEST_FAIL -> throw RemoteException(RemoteException.Type.REQUEST_FAIL)
        BothWayCallback.Result.TIMEOUT -> throw RemoteException(RemoteException.Type.TIMEOUT)
    }
}
