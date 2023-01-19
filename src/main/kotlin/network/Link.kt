package network

import simulator.Simulator
import node.Node
import node.OnlineState

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
                from.checkAndRemovePackagesWithoutRoutingOptions()
            }
        }
    }

    fun startTransmission(nextPackage: Package) {
        if (!isTransmitting() && getOnlineState() && nextPackage.destination.getOnlineState()) {
            val newTransmission = Transmission(linkConfig.transmissionConfig, nextPackage, this)
            newTransmission.start()
            currentTransmission = newTransmission
            Simulator.getMetrics()?.resourcesUsageMetricsCollector?.onLinkOccupied(linkConfig.bandwidth)
        } else if (!nextPackage.destination.getOnlineState()) {
            // todo: note overhead for trying to estabish connection
            from.checkAndRemovePackagesWithoutRoutingOptions()
        }
    }

    fun completeTransmission() {
        val finishedTransmission = currentTransmission
        if (finishedTransmission != null) {
            finishedTransmission.complete()
            from.removePackage(finishedTransmission.p)
            to.receive(finishedTransmission.p)
            currentTransmission = null
            Simulator.getMetrics()?.resourcesUsageMetricsCollector?.onLinkFreedUp(linkConfig.bandwidth)
            sendNextPackage()
        }
    }

    private fun cancelTransmission() {
        val canceledTransmission = currentTransmission
        if (canceledTransmission != null) {
            canceledTransmission.cancel()
            currentTransmission = null
            Simulator.getMetrics()?.resourcesUsageMetricsCollector?.onLinkFreedUp(linkConfig.bandwidth)
            sendNextPackage()
        }
    }

    private fun isTransmitting(): Boolean {
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

    fun currentlyTransmitting(): Package? {
        return currentTransmission?.p
    }
}