import Simulator
import TimedCallback

open class OnlineBehaviour(
    initial: Boolean = false, private val nextOnlineStateChange: ((current: Int, online: Boolean) -> Int?)? = null
) {
    private var online = initial
    private var changeOnlineStateCallback: TimedCallback? = null

    init {
        changeOnlineStateCallback = createSetOnlineCallback()
    }

    open fun changeOnlineState(value: Boolean) {
        online = value
        if (changeOnlineStateCallback != null) {
            Simulator.cancelCallback(changeOnlineStateCallback!!)
        }
        changeOnlineStateCallback = createSetOnlineCallback()
    }

    fun isOnline(): Boolean {
        return online
    }

    private fun createSetOnlineCallback(): TimedCallback? {
        val nextOnlineStateChange = nextOnlineStateChange
        if (nextOnlineStateChange != null) {
            val timestamp = nextOnlineStateChange(Simulator.getCurrentTimestamp(), isOnline())
            if (timestamp != null) {
                val callback = TimedCallback(timestamp) {
                    if (isOnline()) {
                        changeOnlineState(false)
                    } else {
                        changeOnlineState(true)
                    }
                }
                Simulator.addCallback(callback)
                return callback
            }
        }
        return null
    }
}

