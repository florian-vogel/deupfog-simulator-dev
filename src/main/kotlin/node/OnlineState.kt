package node

import simulator.TimedCallback
import simulator.Simulator

open class OnlineState(
    private var state: Boolean = false,
    private val nextOnlineStateChange: ((current: Int, online: Boolean) -> Int?)? = null
) {
    private var changeOnlineStateCallback: TimedCallback? = null

    init {
        changeOnlineStateCallback = createSetOnlineCallback()
    }

    open fun changeOnlineState(value: Boolean) {
        state = value
        resetSetOnlineCallback()
    }

    fun getOnlineState(): Boolean {
        return state
    }

    private fun resetSetOnlineCallback() {
        if (changeOnlineStateCallback != null) {
            Simulator.cancelCallback(changeOnlineStateCallback!!)
        }
        changeOnlineStateCallback = createSetOnlineCallback()
    }

    private fun createSetOnlineCallback(): TimedCallback? {
        val nextOnlineStateChange = nextOnlineStateChange
        if (nextOnlineStateChange != null) {
            val timestamp = nextOnlineStateChange(Simulator.getCurrentTimestamp(), getOnlineState())
            if (timestamp != null) {
                val callback = TimedCallback(timestamp) {
                    if (getOnlineState()) {
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

