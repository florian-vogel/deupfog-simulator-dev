package network

import TimedCallback
import main.Simulator
import Package

data class TransmissionConfig(
    val transmissionOverhead: Int,
    val transmissionTimeCalc: (size: Int, bandwidth: Int, delay: Int) -> Int
)

class Transmission(
    private val transmissionConfig: TransmissionConfig,
    val p: Package,
    private val via: UnidirectionalLink
) {
    private val transmissionCompleteCallback: TimedCallback? = null
    fun start() {
        createTransmissionCompleteCallback()
    }

    fun complete() {}

    fun cancel() {
        if (transmissionCompleteCallback != null)
            Simulator.cancelCallback(transmissionCompleteCallback)
    }

    private fun createTransmissionCompleteCallback() {
        val size = p.getSize() + transmissionConfig.transmissionOverhead
        val transmissionTime =
            transmissionConfig.transmissionTimeCalc(size, via.linkConfig.bandwidth, via.linkConfig.latency)
        val callback = TimedCallback(Simulator.getCurrentTimestamp() + transmissionTime) {
            this.via.completeTransmission()
        }
        Simulator.addCallback(callback)
    }
}
