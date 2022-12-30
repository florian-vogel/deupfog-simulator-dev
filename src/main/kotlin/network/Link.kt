package network

import main.Simulator
import node.Node
import node.OnlineState
import Package

// TODO: think about bidirectional links -> Link Interface needed
// or just as two unidirectional links -> maybe should be both possible -> more generic link interface
// especially how bandwith is consumed should be generic (e.g. seperate bandwiths for each direction or together one)

data class MutableLinkState(
    val isOnline: Boolean
)

data class LinkConfig(
    val bandwidth: Int,
    val latency: Int,
    val transmissionConfig: TransmissionConfig,
    val nextOnlineStateChange: ((current: Int, online: Boolean) -> Int?)? = null,
)

class UnidirectionalLink(
    val linkConfig: LinkConfig, private val from: Node, val to: Node, initialLinkState: MutableLinkState
) : OnlineState(initialLinkState.isOnline, linkConfig.nextOnlineStateChange) {
    private var currentTransmission: Transmission? = null

    override fun changeOnlineState(value: Boolean) {
        if (getOnlineState() != value) {
            super.changeOnlineState(value)
            if (value) {
                sendNextPackage()
            } else {
                cancelTransmission()
            }
        }
        Simulator.metrics!!.linkMetricsCollector.onChangedLinkState(this)
    }

    fun startTransmission(nextPackage: Package) {
        if (!isTransmitting() && getOnlineState()) {
            val newTransmission =
                Transmission(linkConfig.transmissionConfig, nextPackage, this)
            newTransmission.start()
            currentTransmission = newTransmission
        }
        Simulator.metrics?.linkMetricsCollector?.onChangedLinkState(this)
    }

    fun completeTransmission() {
        val finishedTransmission = currentTransmission
        if (finishedTransmission != null) {
            finishedTransmission.complete()
            from.removePackage(finishedTransmission.p)
            to.receive(finishedTransmission.p)
            currentTransmission = null
            sendNextPackage()
        }
        Simulator.metrics?.linkMetricsCollector?.onChangedLinkState(this)
    }

    private fun cancelTransmission() {
        val canceledTransmission = currentTransmission
        if (canceledTransmission != null) {
            canceledTransmission.cancel()
            currentTransmission = null
            sendNextPackage()
        }
        // note: package is not removed from node -> send will be retried, maybe with different link
    }

    fun isTransmitting(): Boolean {
        return currentTransmission != null
    }

    private fun sendNextPackage() {
        if (getOnlineState()) {
            val nextPackage = from.getNextPackage(this)
            if (nextPackage != null) {
                startTransmission(nextPackage)
            }
        }
    }
}