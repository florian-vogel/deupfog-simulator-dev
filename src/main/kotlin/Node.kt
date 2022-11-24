open class InitialNodeState(
    var online: Boolean,
)

open class Node(
    private val capacity: Int, initialNodeState: InitialNodeState = InitialNodeState(false)
) {
    private var online = initialNodeState.online
    private val links: MutableList<UnidirectionalLink> = mutableListOf()

    open fun receive(p: Package) {
        // TODO maybe make payload only accessible by destination node
        if (canReceive()) {
            forwardIfNotDestination(p)
        }
    }

    open fun setOnline(value: Boolean) {
        online = value
    }

    open fun canReceive(): Boolean {
        return online
    }

    fun getOnline(): Boolean {
        return online
    }

    fun forwardIfNotDestination(p: Package) {
        if (!arrivedAtDestination(p, this)) {
            forward(p)
        }
    }

    fun forward(p: Package) {
        // TODO handle no path found exeption consistent
        val nextHop = findShortestPath(this, p.destination)
        getLinkTo(nextHop!!.peek())!!.lineUpPackage(p)
    }

    fun addLink(link: UnidirectionalLink) {
        links.add(link)
    }

    fun getLinks(): List<UnidirectionalLink> {
        return this.links
    }

    fun getLinkTo(node: Node): UnidirectionalLink? {
        return links.find { it.to == node }
    }

    fun getCapacityInUse(): Int {
        return this.links.sumOf { it.elementsWaiting() }
    }
}

data class UpdateRetrievalParams(
    // Push
    val registerAtServerForUpdates: Boolean = false,
    // Pull
    val sendUpdateRequestsInterval: Int? = null
)

abstract class UpdateReceiverNode(
    // TODO: define capacity per link or per node?
    capacity: Int,
    private val responsibleServers: List<Server>,
    // TODO: either increase version of registerd nodes implicitly in server or send updateReceived notification (updates runningSoftware states) when edge received update
    private val runningSoftware: List<SoftwareState>,
    private val updateRetrievalParams: UpdateRetrievalParams,
    initialNodeState: InitialNodeState
) : Node(capacity, initialNodeState) {
    private var pullRequestSchedule: TimedCallback? = null

    override fun receive(p: Package) {
        super.receive(p)
        if (canReceive() && arrivedAtDestination(p, this) && p is UpdatePackage) {
            processUpdate(p.update)
        }
    }

    override fun setOnline(value: Boolean) {
        if (value != getOnline()) {
            if (value) {
                onConnected()
            } else {
                onDisconnected()
            }
        }
        super.setOnline(value)
    }

    open fun processUpdate(update: SoftwareUpdate) {
        Simulator.getUpdateMetrics()?.onArrive(update, this)
        updateRunningSoftware(update)
    }

    open fun listeningFor(): List<SoftwareState> {
        return runningSoftware
    }

    fun registerAtServer(listeningFor: List<SoftwareState>) {
        if (updateRetrievalParams.registerAtServerForUpdates) {
            responsibleServers.forEach {
                val request = RegisterForUpdatesRequest(1, this, it, listeningFor)
                val nextHop = findShortestPath(this, it)?.peek()
                if (nextHop != null) {
                    getLinkTo(nextHop)?.lineUpPackage(request)
                }
            }
        }
    }

    private fun onConnected() {
        // TODO: gucken ob diese parameter überall überprüft werden
        if (updateRetrievalParams.registerAtServerForUpdates) {
            registerAtServer(listeningFor())
        }
        if (updateRetrievalParams.sendUpdateRequestsInterval !== null) {
            initPullRequestSchedule()
        }
    }

    private fun onDisconnected() {
        removePullRequestSchedule()
    }

    private fun updateRunningSoftware(update: SoftwareUpdate) {
        val targetSoftware = runningSoftware::find { it.type == update.type }
        if (targetSoftware != null) {
            targetSoftware.applyUpdate(update)
            registerAtServer(listeningFor())
        }

    }

    private fun sendPullRequestsToResponsibleServers() {
        responsibleServers.forEach {
            val request = PullLatestUpdatesRequest(1, this, it, listeningFor())
            val nextHop = findShortestPath(this, it)?.peek()
            if (nextHop != null) {
                getLinkTo(nextHop)?.lineUpPackage(request)
            }
        }
    }


    private fun initPullRequestSchedule() {
        if (updateRetrievalParams.sendUpdateRequestsInterval != null) {
            Simulator.addCallback(RecursiveCallback(
                Simulator.getCurrentTimestamp(), null, updateRetrievalParams.sendUpdateRequestsInterval
            ) { sendPullRequestsToResponsibleServers() })
        }
    }

    private fun removePullRequestSchedule() {
        pullRequestSchedule = null
    }
}

class InitialServerState(
    online: Boolean, val receivers: List<UpdateReceiverNode>? = null, val updates: List<SoftwareUpdate>? = null
) : InitialNodeState(online)

open class Server(
    capacity: Int,
    responsibleServer: List<Server>,
    // servers can have software which needs to be updated
    runningSoftware: List<SoftwareState>,
    updateRetrievalParams: UpdateRetrievalParams,
    initialServerState: InitialServerState
) : UpdateReceiverNode(capacity, responsibleServer, runningSoftware, updateRetrievalParams, initialServerState) {
    private var receiverRegistry = initialServerState.receivers?.associate { it to it.listeningFor() }?.toMutableMap()

    // servers have additionally to running Software a registry for all received updates which are
    // their clients (updateRecieverNodes) listening to
    private var updateRegistry = initialServerState.updates?.associate { it.type to mutableListOf(it) }?.toMutableMap()

    override fun receive(p: Package) {
        super.receive(p)
        if (!arrivedAtDestination(p, this)) {
            return
        }
        if (p is UpdateRequest) {
            processRequest(p)
        }
    }

    override fun processUpdate(update: SoftwareUpdate) {
        super.processUpdate(update)
        updateUpdateRegistry(update)
        initUpdatePackages()
    }

    override fun listeningFor(): List<SoftwareState> {
        val runningSoftware = super.listeningFor()
        val serverNodeListeningFor = getCurrentUpdateRegistryStates() + getCurrentReceiverRegistryStates()
        val combined = runningSoftware + serverNodeListeningFor
        val oneValueForEachType = mutableListOf<SoftwareState>()
        combined.forEach { combinedValue ->
            if (!oneValueForEachType.map { it.type }.contains(combinedValue.type)) {
                oneValueForEachType.add(combinedValue)
            } else {
                val x = oneValueForEachType.find { combinedValue.type == it.type }
                if (x != null && x.versionNumber < combinedValue.versionNumber) {
                    oneValueForEachType.remove(x)
                    oneValueForEachType.add(combinedValue)
                }
            }
        }
        return oneValueForEachType
    }

    private fun processRequest(request: UpdateRequest) {
        if (request is PullLatestUpdatesRequest) {
            initUpdatePackageAndPassToLink(request.initialPosition, request.softwareStates)
        } else if (request is RegisterForUpdatesRequest) {
            registerNodeInLocalRegistry(request.initialPosition, request.softwareStates)
        }
    }

    private fun initUpdatePackages() {
        receiverRegistry?.forEach {
            initUpdatePackageAndPassToLink(it.key, it.value)
        }
    }

    private fun updateUpdateRegistry(update: SoftwareUpdate) {
        if (updateRegistry?.get(update.type) == null) {
            updateRegistry = mutableMapOf(update.type to mutableListOf(update))
        } else {
            updateRegistry!![update.type]!!.add(update)
        }
    }

    private fun getCurrentUpdateRegistryStates(): List<SoftwareState> {
        val states = mutableListOf<SoftwareState>()
        updateRegistry?.forEach {
            val state = SoftwareState(it.key, 0, 0)
            it.value.sortBy { update -> update.updatesToVersion }
            it.value.forEach { update -> state.applyUpdate(update) }
            states.add(state)
        }
        return states
    }

    private fun getCurrentReceiverRegistryStates(): List<SoftwareState> {
        val states = mutableListOf<SoftwareState>()
        receiverRegistry?.forEach {
            states += it.value
        }
        return states
    }

    private fun initUpdatePackageAndPassToLink(
        target: UpdateReceiverNode, targetSoftwareStates: List<SoftwareState>
    ) {
        targetSoftwareStates.map {
            updateRegistry?.get(it.type)?.filter { update ->
                it.type.updateCompatible(
                    it.versionNumber, update.updatesToVersion
                )
            }?.maxByOrNull { compatibleUpdate -> compatibleUpdate.updatesToVersion }
        }.forEach {
            if (it != null) {
                this.getLinkTo(target)?.lineUpPackage(
                    UpdatePackage(this, target, it.size, it)
                )
            }
        }
    }

    private fun registerNodeInLocalRegistry(
        node: UpdateReceiverNode, listeningFor: List<SoftwareState>?
    ) {
        if (receiverRegistry == null) {
            receiverRegistry = mutableMapOf()
        }

        if (listeningFor != null) {
            receiverRegistry?.set(node, listeningFor.toMutableList())
            initUpdatePackageAndPassToLink(node, listeningFor)
            registerAtServer(listeningFor())
        } else {
            receiverRegistry?.set(node, mutableListOf())
        }
    }
}

class Edge(
    maxElements: Int,
    responsibleUpdateServer: List<Server>,
    runningSoftware: List<SoftwareState>,
    updateRetrievalParams: UpdateRetrievalParams,
    initialNodeState: InitialNodeState
) : UpdateReceiverNode(
    maxElements, responsibleUpdateServer, runningSoftware, updateRetrievalParams, initialNodeState
) {}
