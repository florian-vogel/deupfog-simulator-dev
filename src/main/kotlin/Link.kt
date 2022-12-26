import main.Simulator
import node.Node

// TODO: think about bidirectional links -> Link Interface needed
// or just as two unidirectional links -> maybe should be both possible -> more generic link interface
// especially how bandwith is consumed should be generic (e.g. seperate bandwiths for each direction or together one)

// todo:
// think about creating network class which specifies
// parameters that should be globally equal (maybe like the calculation of the
// transfer time)
// nodes, links are components of the network, the network is passed into the simulation
// also, leave transmission calculation as input param

data class MutableLinkState(
    val isOnline: Boolean
)

data class LinkSimParams(
    val bandwidth: Int, val latency: Int, val nextOnlineStateChange: ((current: Int, online: Boolean) -> Int?)? = null
)

class UnidirectionalLink(
    private val simParams: LinkSimParams, val to: Node, initialLinkState: MutableLinkState
) : OnlineState(initialLinkState.isOnline, simParams.nextOnlineStateChange) {
    private var getNextPackage: (UnidirectionalLink) -> Package? = { _ -> null }
    private var onTransmissionSuccessful: (Package) -> Unit = { }
    private var currentTransmission: Transmission? = null

    override fun changeOnlineState(value: Boolean) {
        if (getOnlineState() != value) {
            super.changeOnlineState(value)
            println("new link state: $value")
            if (value) {
                sendNextPackage()
            } else {
                currentTransmission?.cancelTransmitting()
            }
        }
        Simulator.metrics!!.linkMetricsCollector.onChangedLinkState(this)
    }

    fun initializeFromParams(
        getNextPackage: (UnidirectionalLink) -> Package?,
        onTransmissionSuccessful: (Package) -> Unit
    ) {
        this.getNextPackage = getNextPackage
        this.onTransmissionSuccessful = onTransmissionSuccessful
    }

    fun onTransmissionFinished() {
        val finishedTransmission = currentTransmission
        if (finishedTransmission != null) {
            onTransmissionSuccessful(finishedTransmission.p)
            currentTransmission = null
            sendNextPackage()
        }
        Simulator.metrics?.linkMetricsCollector?.onChangedLinkState(this)
    }

    fun transmissionCanceled() {
        if (currentTransmission != null) {
            // package not removed from node, send will be retried, maybe with different link
            currentTransmission = null
        }
    }

    fun tryTransmission(nextPackage: Package) {
        if (hasUnusedBandwidth() && getOnlineState()) {
            // TODO: leave transmissionTime calculation as input parameter
            val transmissionTime = nextPackage.getSize() / simParams.bandwidth + simParams.latency * 2
            currentTransmission = SimpleTransmission(nextPackage, transmissionTime, this)
        }
        Simulator.metrics?.linkMetricsCollector?.onChangedLinkState(this)
    }

    fun hasUnusedBandwidth(): Boolean {
        return currentTransmission === null
    }

    private fun sendNextPackage() {
        if (getOnlineState()) {
            val nextPackage = getNextPackage(this)
            if (nextPackage != null) {
                tryTransmission(nextPackage)
            }
        }
    }
}