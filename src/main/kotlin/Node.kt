import Software.SoftwareState
import Software.SoftwareUpdate
import Software.Software
import Software.applyUpdates
import java.util.LinkedList

open class MutableNodeState(
    val online: Boolean,
)

open class NodeSimParams(
    val storageCapacity: Int,
    val nextOnlineStateChange: ((current: Int, online: Boolean) -> Int?)? = null,
    val calculateProcessingTime: ((p: Package) -> Int)? = null
)

open class Node(
    // default initially online false, if set to true initially, edges need to be registered manually at their servers
    // this behaviour might be consistent but strange
    // => when starting offline the clear
    // => when starting online treat the network as an
    // initial goOnline, but a starting state can still be
    // configured if nessasary
    private val simParams: NodeSimParams, initialNodeState: MutableNodeState
) : OnlineState(initialNodeState.online, simParams.nextOnlineStateChange) {
    private val links: MutableList<UnidirectionalLink> = mutableListOf()
    private val packageQueue = LinkedList<Package>()


    open fun receive(p: Package) {
        if (!getOnlineState()) return;
        if (p.destination != this) {
            addToPackageQueue(p)
        }
    }

    override fun changeOnlineState(value: Boolean) {
        super.changeOnlineState(value)
        Simulator.metrics?.nodeMetricsCollector?.onNodeStateChanged(this)
    }

    open fun addLink(link: UnidirectionalLink) {
        links.add(link)
        link.initializeFromParams({ getNextPackage(it) }) { p -> this.removePackage(p) }
    }

    open fun removeLink(link: UnidirectionalLink) {
        links.remove(link)
    }

    fun getLinks(): List<UnidirectionalLink>? {
        if (!getOnlineState()) return null;
        return this.links.filter { it.getOnlineState() }
    }

    fun getLinkTo(node: Node): UnidirectionalLink? {
        if (!getOnlineState()) return null;
        return links.find { it.to == node && it.getOnlineState() }
    }

    protected fun addToPackageQueue(p: Package) {
        this.packageQueue.add(p)
        val nextHop = findShortestPath(this, p.destination)?.firstOrNull()
        if (nextHop != null) {
            links.find { it.to == nextHop }?.tryTransmission(p)
        }
    }

    private fun getFreeStorageCapacity(): Int {
        return this.simParams.storageCapacity - this.packageQueue.sumOf { it.getSize() }
    }


    private fun getNextPackage(link: UnidirectionalLink): Package? {
        val queueCopy = packageQueue.toList()
        queueCopy.forEach {
            val nextHop = findShortestPath(this, it.destination)?.firstOrNull()
            if (nextHop == null) {
                packageLost(it)
            } else if (nextHop == link.to) {
                return it
            }
        }
        return null
    }

    private fun removePackage(p: Package) {
        this.packageQueue.remove(p)
    }

    private fun packageLost(p: Package) {
        removePackage(p)
        Simulator.metrics!!.nodeMetricsCollector.onPackageLost(this)
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
    // either increase version of registered nodes implicitly in server or send updateReceived notification (updates runningSoftware states) when edge received update
    // --> currently sending update received notification
    private val runningSoftware: List<SoftwareState>,
    private val updateRetrievalParams: UpdateRetrievalParams,
    initialNodeState: MutableNodeState
) : Node(nodeSimParams, initialNodeState) {
    private var pullRequestSchedule: TimedCallback? = null

    init {
        if (initialNodeState.online) {
            initStrategy()
        }
    }

    override fun receive(p: Package) {
        if (!getOnlineState()) return;
        if (p is UpdatePackage && p.destination == this) {
            processUpdate(p.update)
        } else {
            addToPackageQueue(p)
        }
    }

    open fun processUpdate(update: SoftwareUpdate) {
        updateRunningSoftware(update)
        Simulator.getUpdateMetrics()?.onArrive(update, this)
    }

    override fun changeOnlineState(value: Boolean) {
        val onlineStatusChanged = value != getOnlineState();
        if (onlineStatusChanged) {
            super.changeOnlineState(value)
            if (value) {
                initStrategy()
            } else {
                cancelStrategy()
            }
        }
    }

    private fun initStrategy() {
        registerAtServers(listeningFor())
        makePullRequestsRecursive()
    }

    protected fun registerAtServers(listeningFor: List<SoftwareState>) {
        if (updateRetrievalParams.registerAtServerForUpdates) {
            responsibleServers.forEach {
                registerAtServer(it, listeningFor)
            }
        }
    }

    private fun cancelStrategy() {
        removePullRequestSchedule()
    }

    override fun addLink(link: UnidirectionalLink) {
        super.addLink(link)
        if (link.getOnlineState()) {
            // todo: refactor
            if (link.to is Server) {
                registerAtServer(link.to, listeningFor())
            }
        }
    }


    open fun listeningFor(): List<SoftwareState> {
        return runningSoftware
    }


    private fun updateRunningSoftware(update: SoftwareUpdate) {
        val targetSoftware = runningSoftware::find { it.type == update.type }
        if (targetSoftware != null) {
            targetSoftware.applyUpdate(update)
            registerAtServers(listeningFor())
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
            schedulePullRequest(updateRetrievalParams.sendUpdateRequestsInterval)
        }
    }

    private fun schedulePullRequest(nextRequestIn: Int) {
        pullRequestSchedule = TimedCallback(Simulator.getCurrentTimestamp() + nextRequestIn) {
            makePullRequestsRecursive()
        }
        Simulator.addCallback(pullRequestSchedule!!)
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

class MutableServerState(
    online: Boolean,
    val subscriberRegistry: MutableMap<UpdateReceiverNode, MutableList<SoftwareState>> = mutableMapOf(),
    val updateRegistry: MutableMap<Software, MutableList<SoftwareUpdate>> = mutableMapOf(),
) : MutableNodeState(online)

open class Server(
    nodeSimParams: NodeSimParams,
    responsibleServer: List<Server>,
    // todo: servers can have software which needs to be updated --> eventuell klarere trennung zwischen sender und receiver
    runningSoftware: List<SoftwareState>,
    updateRetrievalParams: UpdateRetrievalParams,
    initialServerState: MutableServerState
) : UpdateReceiverNode(
    nodeSimParams, responsibleServer, runningSoftware, updateRetrievalParams, initialServerState
) {
    private var currentState: MutableServerState? = initialServerState;
    private val recentPullNodesRegistry: MutableMap<UpdateReceiverNode, MutableList<SoftwareState>> = mutableMapOf()

    override fun receive(p: Package) {
        super.receive(p)
        if (p is UpdateRequest && p.destination == this) {
            processRequest(p)
        }
    }

    override fun processUpdate(update: SoftwareUpdate) {
        super.processUpdate(update)
        updateUpdateRegistry(update)
        initUpdatePackagesToAllSubscribers()
    }

    override fun listeningFor(): List<SoftwareState> {
        // TODO: refactor this
        val runningSoftware = super.listeningFor()
        val serverNodeListeningFor =
            getCurrentUpdateRegistryStates() + getCurrentSubscriberRegistryStates() + getCurrentRecentPullNodesRegistryStates()
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
        if (link.getOnlineState()) {
            if (link.to is UpdateReceiverNode) {
                val receiverRegistryEntry = currentState?.subscriberRegistry?.get(link.to)
                if (receiverRegistryEntry != null) {
                    initUpdatePackageAndPassToLink(link.to, receiverRegistryEntry)
                }
            }
        }
    }

    override fun removeLink(link: UnidirectionalLink) {
        super.removeLink(link)
        // todo: check if other links exist
        currentState?.subscriberRegistry?.remove(link.to)
    }

    private fun processRequest(request: UpdateRequest) {
        if (request is PullLatestUpdatesRequest) {
            // todo: bei pull keine registrierung, macht nur sinn, wenn es wirklich nur einen server gibt,
            // sonst kommt das update nie beim sub-server an, da dieser es nicht anfragt
            // alternativ auch in subscriber registry speichern aber über meta daten steuern, dass kein update
            // gepushed wird
            // initUpdatePackageAndPassToLink(request.initialPosition, request.softwareStates)
            sendAvailableUpdatesAndRegisterNodeInLocalRegistry(
                request.initialPosition as UpdateReceiverNode, request.softwareStates, registerAsSubscriber = false
            )
        } else if (request is RegisterForUpdatesRequest) {
            sendAvailableUpdatesAndRegisterNodeInLocalRegistry(
                request.initialPosition as UpdateReceiverNode, request.softwareStates, registerAsSubscriber = true
            )
        }
    }

    private fun initUpdatePackagesToAllSubscribers() {
        currentState?.subscriberRegistry?.forEach {
            initUpdatePackageAndPassToLink(it.key, it.value)
        }
    }

    private fun updateUpdateRegistry(update: SoftwareUpdate) {
        // todo: updateing update registry might be linked to sendOfUpdatesToSubs
        // show link physically
        val registry = currentState?.updateRegistry?.get(update.type);
        if (registry == null) {
            // todo: can I replace this with registry = ...
            currentState?.updateRegistry?.set(update.type, mutableListOf(update))
        } else {
            registry.add(update)
        }
    }

    private fun getCurrentUpdateRegistryStates(): List<SoftwareState> {
        val updateRegistry = currentState?.updateRegistry?.toList() ?: return emptyList()
        return updateRegistry.map {
            applyUpdates(it.first, it.second)
        }
    }

    private fun getCurrentRecentPullNodesRegistryStates(): List<SoftwareState> {
        // todo: remove, maybe refactor whole registry approach,
        // just save where to send to and what to listenFor
        // don't care about push pull nodes or subscribers
        if (recentPullNodesRegistry == null) return emptyList();
        return recentPullNodesRegistry.map { it.value }.flatten()
    }

    private fun getCurrentSubscriberRegistryStates(): List<SoftwareState> {
        val state = currentState ?: return emptyList()
        return state.subscriberRegistry.map { it.value }.flatten()
    }

    private fun initUpdatePackageAndPassToLink(
        target: UpdateReceiverNode, targetSoftwareStates: List<SoftwareState>
    ) {
        val currentStateNonNull = currentState ?: return
        targetSoftwareStates.map {
            currentStateNonNull.updateRegistry[it.type]?.filter { update ->
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

    private fun sendAvailableUpdatesAndRegisterNodeInLocalRegistry(
        node: UpdateReceiverNode, listeningFor: List<SoftwareState>, registerAsSubscriber: Boolean
    ) {
        if (registerAsSubscriber) {
            currentState?.subscriberRegistry?.set(node, listeningFor.toMutableList())
            // TODO: only send those which the server has
            initUpdatePackageAndPassToLink(node, listeningFor)
        } else {
            recentPullNodesRegistry[node] = listeningFor.toMutableList()
            initUpdatePackageAndPassToLink(node, listeningFor)
        }
        // TODO: register only for those which server wasn't listening before
        registerAtServers(listeningFor())
    }

    fun initializeUpdate(updatePackage: UpdatePackage) {
        processUpdate(updatePackage.update)
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
