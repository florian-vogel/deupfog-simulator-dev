import java.util.LinkedList

open class MutableNodeState(
    val online: Boolean,
)

open class NodeSimParams(
    val capacity: Int,
    val nextOnlineStateChange: ((current: Int, online: Boolean) -> Int?)? = null,
    // val calculateProcessingTime: ((p: Package) -> Int)? = null
)

open class Node(
    // default initially online false, if set to true initially, edges need to be registered manually at their servers
    simParams: NodeSimParams, initialNodeState: MutableNodeState = MutableNodeState(false)
) : OnlineBehaviour(initialNodeState.online, simParams.nextOnlineStateChange) {
    private val links: MutableList<UnidirectionalLink> = mutableListOf()
    private val packageQueue = LinkedList<Package>()

    init {
    }

    override fun changeOnlineState(value: Boolean) {
        super.changeOnlineState(value)
        Simulator.metrics!!.nodeMetricsCollector.onNodeStateChanged(this)
    }

    open fun receive(p: Package) {
        if (isOnline() && p.destination != this) {
            lineUpPackage(p)
        }
    }

    open fun addLink(link: UnidirectionalLink) {
        if (link.isOnline()) {
            links.add(link)
            link.setGetNextPackage { getNextPackage(it) }
        }
    }

    open fun removeLink(link: UnidirectionalLink) {
        links.remove(link)
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
        val queueCopy = packageQueue.toList()
        queueCopy.forEach {
            val nextHop = findShortestPath(this, it.destination)?.firstOrNull()
            if (nextHop == null) {
                packageQueue.remove(it)
                Simulator.metrics!!.nodeMetricsCollector.onPackageLost(this)
            } else if (nextHop == link.to) {
                return it
            }
        }
        return null
    }

    private fun lineUpPackage(p: Package) {
        this.packageQueue.add(p)
        val nextHop = findShortestPath(this, p.destination)?.firstOrNull()
        if (nextHop != null) {
            links.find { it.to == nextHop }?.tryTransmission(p)
        }
    }
}

// TODO: eventuell jede data distribution strategy in eigener node-klasse
data class UpdateRetrievalParams(
    // Push
    val registerAtServerForUpdates: Boolean = false,
    // Pull
    val sendUpdateRequestsInterval: Int? = null
)

// TODO: In Push und Pull node trennen, update retrival params entfernen
abstract class UpdateReceiverNode(
    nodeSimParams: NodeSimParams,
    private val responsibleServers: List<Server>,
    // either increase version of registered nodes implicitly in server or send updateReceived notification (updates runningSoftware states) when edge received update
    // --> currently sending update received notification
    private val runningSoftware: List<SoftwareState>,
    private val updateRetrievalParams: UpdateRetrievalParams,
    initialNodeState: MutableNodeState
) : Node(nodeSimParams, initialNodeState) {
    private var pullRequestSchedule: TimedCallback? = null

    override fun receive(p: Package) {
        super.receive(p)
        if (p is UpdatePackage && isOnline() && arrivedAtDestination(p, this)) {
            processUpdate(p.update)
        }
    }

    override fun changeOnlineState(value: Boolean) {
        val onlineStatusChanged = value != isOnline();
        super.changeOnlineState(value)
        if (onlineStatusChanged) {
            if (value) {
                onConnected()
            } else {
                onDisconnected()
            }
        }
    }

    override fun addLink(link: UnidirectionalLink) {
        super.addLink(link)
        if (link.isOnline()) {
            if (link.to is Server) {
                registerAtServer(link.to, listeningFor())
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
            registerAtServers(listeningFor())
            makePullRequestsRecursive()
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
            registerAtServers(listeningFor())
        }

    }

    protected fun registerAtServers(listeningFor: List<SoftwareState>) {
        if (updateRetrievalParams.registerAtServerForUpdates) {
            responsibleServers.forEach {
                registerAtServer(it, listeningFor)
            }
        }
    }

    private fun registerAtServer(server: Server, listeningFor: List<SoftwareState>) {
        if (updateRetrievalParams.registerAtServerForUpdates && listeningFor.isNotEmpty()) {
            // TODO: leave as input
            val requestPackageSize = 1
            val request = RegisterForUpdatesRequest(requestPackageSize, this, server, listeningFor)
            val nextHop = findShortestPath(this, server)?.peek()
            if (nextHop != null) {
                receive(request)
            }
        }
    }

    private fun removePullRequestSchedule() {
        if (pullRequestSchedule != null) {
            Simulator.cancelCallback(pullRequestSchedule!!)
        }
        pullRequestSchedule = null
    }

    private fun makePullRequestsRecursive() {
        if (updateRetrievalParams.sendUpdateRequestsInterval != null) {
            sendPullRequestsToResponsibleServers()
            pullRequestSchedule =
                TimedCallback(Simulator.getCurrentTimestamp() + updateRetrievalParams.sendUpdateRequestsInterval) {
                    makePullRequestsRecursive()
                }
            Simulator.addCallback(pullRequestSchedule!!)
        }
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
) : MutableNodeState(online)

open class Server(
    nodeSimParams: NodeSimParams,
    responsibleServer: List<Server>,
    // servers can have software which needs to be updated --> eventuell klarere trennung zwischen sender und receiver
    runningSoftware: List<SoftwareState>,
    updateRetrievalParams: UpdateRetrievalParams,
    initialServerState: InitialServerState
) : UpdateReceiverNode(
    nodeSimParams, responsibleServer, runningSoftware, updateRetrievalParams, initialServerState
) {
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

    override fun addLink(link: UnidirectionalLink) {
        super.addLink(link)
        if (link.isOnline()) {
            if (link.to is UpdateReceiverNode) {
                val receiverRegistryEntry = receiverRegistry?.get(link.to)
                if (receiverRegistryEntry != null) {
                    initUpdatePackageAndPassToLink(link.to, receiverRegistryEntry)
                }
            }
        }
    }

    override fun removeLink(link: UnidirectionalLink) {
        super.removeLink(link)
        receiverRegistry?.remove(link.to)
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
        //
        if (receiverRegistry == null) {
            receiverRegistry = mutableMapOf()
        }

        if (listeningFor != null) {
            receiverRegistry?.set(node, listeningFor.toMutableList())
            // TODO: only send those which the server has
            initUpdatePackageAndPassToLink(node, listeningFor)
            // TODO: register only for those which server wasn't listening before
            registerAtServers(listeningFor())
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
    initialNodeState: MutableNodeState
) : UpdateReceiverNode(
    nodeSimParams, responsibleUpdateServer, runningSoftware, updateRetrievalParams, initialNodeState
) {}
