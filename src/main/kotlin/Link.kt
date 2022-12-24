// TODO: think about bidirectional links -> Link Interface needed
// or just as two unidirectional links -> maybe should be both possible -> more generic link interface
// especially how bandwith is consumed should be generic (e.g. seperate bandwiths for each direction or together one)

data class MutableLinkState(
    val isOnline: Boolean
)

data class LinkSimParams(
    val bandwidth: Int, val latency: Int, val nextOnlineStateChange: ((current: Int, online: Boolean) -> Int?)? = null
)

// an sich wird isOnline nicht mehr benötigt, denn links entfernen sich selbst vom node, wenn sie offline gehen
class UnidirectionalLink(
    private val simParams: LinkSimParams, private val from: Node, val to: Node, initialLinkState: MutableLinkState
) : OnlineState(initialLinkState.isOnline, simParams.nextOnlineStateChange) {
    private var getNextPackage: (UnidirectionalLink) -> Package? = { _ -> null }
    private var currentTransmission: Transmission? = null

    init {
        if (getOnlineState()) {
            from.addLink(this)
        }
    }

    override fun changeOnlineState(value: Boolean) {
        if (getOnlineState() != value) {
            super.changeOnlineState(value)
            println("new link state: $value")
            if (value) {
                from.addLink(this)
                sendNext()
            } else {
                from.removeLink(this)
                currentTransmission?.cancelTransmitting()
            }
        }
        Simulator.metrics!!.linkMetricsCollector.onChangedLinkState(this)
    }

    fun onTransmissionFinished() {
        if (currentTransmission != null) {
            from.removePackage(currentTransmission!!.p)
            currentTransmission = null
            sendNext()
        }
        Simulator.metrics!!.linkMetricsCollector.onChangedLinkState(this)
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
            var size = nextPackage.size
            if (nextPackage is UpdatePackage) {
                size += nextPackage.update.size
            }
            val transmissionTime = size / simParams.bandwidth + simParams.latency * 2
            currentTransmission = SimpleTransmission(nextPackage, transmissionTime, this)
        }
        Simulator.metrics!!.linkMetricsCollector.onChangedLinkState(this)
    }

    fun hasUnusedBandwidth(): Boolean {
        return currentTransmission === null
    }

    fun setGetNextPackage(getNext: (UnidirectionalLink) -> Package?) {
        getNextPackage = getNext
    }

    private fun sendNext() {
        if (getOnlineState()) {
            val nextPackage = getNextPackage(this)
            if (nextPackage != null) {
                tryTransmission(nextPackage)
            }
        }
    }
}