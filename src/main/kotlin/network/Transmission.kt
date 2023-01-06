package network

import simulator.TimedCallback
import simulator.Simulator

data class TransmissionConfig(
    val transmissionOverhead: Int, val transmissionTimeCalc: (size: Int, bandwidth: Int, delay: Int) -> Int
)

class Transmission(
    private val transmissionConfig: TransmissionConfig, val p: Package, private val via: UnidirectionalLink
) {
    private val transmissionCompleteCallback: TimedCallback? = null
    fun start() {
        createTransmissionCompleteCallback()
    }

    fun complete() {
        Simulator.getMetrics()?.resourcesUsageMetricsCollector?.packageArrivedSuccessfully(
            calculateTotalBandwidthUsage()
        )
    }

    fun cancel() {
        if (transmissionCompleteCallback != null) Simulator.cancelCallback(transmissionCompleteCallback)
    }

    private fun createTransmissionCompleteCallback() {
        val transmissionTime = transmissionConfig.transmissionTimeCalc(
            calculateTotalBandwidthUsage(), via.linkConfig.bandwidth, via.linkConfig.latency
        )
        val callback = TimedCallback(Simulator.getCurrentTimestamp() + transmissionTime) {
            this.via.completeTransmission()
        }
        Simulator.addCallback(callback)
    }

    private fun calculateTotalBandwidthUsage(): Int {
        return p.getSize() + transmissionConfig.transmissionOverhead;
    }
}
