import java.util.LinkedList

open class InitialNodeState(
    var online: Boolean,
)

open class NodeSimParams(
    val capacity: Int, val nextOfflineTimestamp: ((current: Int) -> Int)? = null
)

open class Node(
    // default initially online false, if set to true initially, edges need to be registered manually at their servers
    private val simParams: NodeSimParams, initialNodeState: InitialNodeState = InitialNodeState(false)
) {
    private var online = initialNodeState.online
    private val links: MutableList<UnidirectionalLink> = mutableListOf()
    private val packageQueue = LinkedList<Package>()

    open fun receive(p: Package) {
        if (isOnline() && p.destination != this) {
            lineUpPackage(p)
        }
    }

    // TODO: outsource availability functionality into interface (open class)
    open fun setOnline(value: Boolean) {
        online = value
    }

    fun isOnline(): Boolean {
        return online
    }

    fun addLink(link: UnidirectionalLink) {
        links.add(link)
        // TODO: see if this works
        link.setGetNextPackage { getNextPackage(it) }
    }

    fun getLinks(): List<UnidirectionalLink> {
        return this.links
    }

    fun getLinkTo(node: Node): UnidirectionalLink? {
        return links.find { it.to == node }
    }

    fun getCapacityInUse(): Int {
        return this.packageQueue.sumOf { it.size }
    }

    fun removePackage(p: Package) {
        this.packageQueue.remove(p)
    }

    private fun getNextPackage(link: UnidirectionalLink): Package? {
        packageQueue.forEach {
            val nextHop = findShortestPath(this, it.destination)?.firstOrNull()
            if (nextHop == link.to) {
                return it
            }
        }
        return null
    }

    private fun lineUpPackage(p: Package) {
        this.packageQueue.add(p)
        val nextHop = findShortestPath(this, p.destination)?.firstOrNull()
        if (nextHop != null) {
            getLinkTo(nextHop)?.tryTransmission(p)
        }
    }
}

data class UpdateRetrievalParams(
    // Push
    val registerAtServerForUpdates: Boolean = false,
    // Pull
    val sendUpdateRequestsInterval: Int? = null
)

abstract class UpdateReceiverNode(
    nodeSimParams: NodeSimParams,
    private val responsibleServers: List<Server>,
    // TODO: either increase version of registerd nodes implicitly in server or send updateReceived notification (updates runningSoftware states) when edge received update
    private val runningSoftware: List<SoftwareState>,
    private val updateRetrievalParams: UpdateRetrievalParams,
    initialNodeState: InitialNodeState
) : Node(nodeSimParams, initialNodeState) {
    private var pullRequestSchedule: TimedCallback? = null

    override fun receive(p: Package) {
        super.receive(p)
        if (p is UpdatePackage && isOnline() && arrivedAtDestination(p, this)) {
            processUpdate(p.update)
        }
    }

    override fun setOnline(value: Boolean) {
        val onlineStatusChanged = value != isOnline();
        super.setOnline(value)
        if (onlineStatusChanged) {
            if (value) {
                onConnected()
            } else {
                onDisconnected()
            }
        }
    }

    open fun processUpdate(update: SoftwareUpdate) {
        Simulator.getUpdateMetrics()?.onArrive(update, this)
        updateRunningSoftware(update)
    }

    open fun listeningFor(): List<SoftwareState> {
        return runningSoftware
    }

    private fun onConnected() {
        if (isOnline()) {
            registerAtServer(listeningFor())
            initPullRequestSchedule()
        } else {
            throw Exception("called onConnected but isOnline() returned false")
        }
    }

    private fun onDisconnected() {
        if (!isOnline()) {
            removePullRequestSchedule()
        } else {
            throw Exception("called onDisconnected but isOnline() returned true")
        }
    }

    private fun updateRunningSoftware(update: SoftwareUpdate) {
        val targetSoftware = runningSoftware::find { it.type == update.type }
        if (targetSoftware != null) {
            targetSoftware.applyUpdate(update)
            registerAtServer(listeningFor())
        }

    }

    protected fun registerAtServer(listeningFor: List<SoftwareState>) {
        if (updateRetrievalParams.registerAtServerForUpdates) {
            responsibleServers.forEach {
                val request = RegisterForUpdatesRequest(1, this, it, listeningFor)
                val nextHop = findShortestPath(this, it)?.peek()
                if (nextHop != null) {
                    receive(request)
                }
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
        pullRequestSchedule?.cancelCallback()
        pullRequestSchedule = null
    }

    private fun sendPullRequestsToResponsibleServers() {
        responsibleServers.forEach {
            val request = PullLatestUpdatesRequest(1, this, it, listeningFor())
            val nextHop = findShortestPath(this, it)?.peek()
            if (nextHop != null) {
                receive(request)
            }
        }
    }
}

class InitialServerState(
    online: Boolean, val receivers: List<UpdateReceiverNode>? = null, val updates: List<SoftwareUpdate>? = null
) : InitialNodeState(online)

open class Server(
    nodeSimParams: NodeSimParams,
    responsibleServer: List<Server>,
    // servers can have software which needs to be updated
    runningSoftware: List<SoftwareState>,
    updateRetrievalParams: UpdateRetrievalParams,
    initialServerState: InitialServerState
) : UpdateReceiverNode(nodeSimParams, responsibleServer, runningSoftware, updateRetrievalParams, initialServerState) {
    private var receiverRegistry = initialServerState.receivers?.associate { it to it.listeningFor() }?.toMutableMap()
    private var updateRegistry = initialServerState.updates?.associate { it.type to mutableListOf(it) }?.toMutableMap()

    override fun receive(p: Package) {
        super.receive(p)
        if (p is UpdateRequest && p.destination == this) {
            processRequest(p)
        }
    }

    override fun processUpdate(update: SoftwareUpdate) {
        super.processUpdate(update)
        updateUpdateRegistry(update)
        initUpdatePackages()
    }

    override fun listeningFor(): List<SoftwareState> {
        // TODO: refactor this
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
                receive(
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
    nodeSimParams: NodeSimParams,
    responsibleUpdateServer: List<Server>,
    runningSoftware: List<SoftwareState>,
    updateRetrievalParams: UpdateRetrievalParams,
    initialNodeState: InitialNodeState
) : UpdateReceiverNode(
    nodeSimParams, responsibleUpdateServer, runningSoftware, updateRetrievalParams, initialNodeState
) {}
