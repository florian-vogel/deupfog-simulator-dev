open class Node(
    private val capacity: Int, private val links: MutableList<UnidirectionalLink> = mutableListOf(),
) {

    open fun receive(p: Package) {
        // TODO maybe make payload only accessible by destination node
        if (!arrivedAtDestination(p, this)) {
            forward(p)
        }
    }

    open fun addLink(link: UnidirectionalLink) {
        links.add(link)
    }

    fun forward(p: Package) {
        // TODO handle no path found exeption consistent
        val nextHop = findShortestPath(this, p.destination)
        getLinkTo(nextHop!!.peek())!!.lineUpPackage(p)
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
    capacity: Int,
    private val responsibleServers: List<Server>,
    private val runningSoftware: List<SoftwareState>,
    private val updateRetrievalParams: UpdateRetrievalParams,
    links: MutableList<UnidirectionalLink> = mutableListOf(),
) : Node(capacity, links) {

    private var pullRequestSchedule: TimedCallback? = null

    init {
        // TODO: does this whole onconnected functionality make sense
        onConnected()
    }

    override fun receive(p: Package) {
        super.receive(p)
        if (!arrivedAtDestination(p, this)) {
            return
        }
        if (p is UpdatePackage) {
            processUpdate(p.update)
        }
    }

    override fun addLink(link: UnidirectionalLink) {
        super.addLink(link)
        registerAtServerForNewLink(link)
    }

    open fun processUpdate(update: SoftwareUpdate) {
        updateRunningSoftware(update)
    }

    open fun listeningFor(): List<SoftwareState> {
        return runningSoftware
    }

    fun onConnected() {
        // TODO: make it possible to initialize with already registered nodes
        if (updateRetrievalParams.registerAtServerForUpdates) {
            registerAtServer()
        }
        if (updateRetrievalParams.sendUpdateRequestsInterval !== null) {
            initPullRequestSchedule()
        }
    }

    fun onDisconnected() {
        removePullRequestSchedule()
    }

    private fun updateRunningSoftware(update: SoftwareUpdate) {
        // TODO: see if this works
        runningSoftware.find { it.type == update.type }?.applyUpdate(update)
    }

    private fun registerAtServer() {
        responsibleServers.forEach {
            val request = RegisterForUpdatesRequest(1, this, it, listeningFor())
            val nextHop = findShortestPath(this, it)?.peek()
            if (nextHop != null) {
                getLinkTo(nextHop)?.lineUpPackage(request)
            }
        }
    }

    private fun registerAtServerForNewLink(newLink: UnidirectionalLink) {
        if (newLink.to is Server && updateRetrievalParams.registerAtServerForUpdates) {
            val request = RegisterForUpdatesRequest(1, this, newLink.to, listeningFor())
            newLink.lineUpPackage(request)
        }
    }

    private fun sendPullRequest() {
        responsibleServers.forEach {
            val request = PullLatestUpdatesRequest(1, this, it, listeningFor())
            val nextHop = findShortestPath(this, it)?.peek()
            if (nextHop != null) {
                getLinkTo(nextHop)?.lineUpPackage(request)
            }
        }
    }


    private fun initPullRequestSchedule() {
        // TODO
    }

    private fun removePullRequestSchedule() {
        pullRequestSchedule = null
    }

}

// TODO: server und updateReceiverNode trennen
open class Server(
    capacity: Int,
    responsibleServer: List<Server>,
    // servers can have software which needs to be updated
    runningSoftware: List<SoftwareState>,
    updateRetrievalParams: UpdateRetrievalParams,
    links: MutableList<UnidirectionalLink> = mutableListOf(),
) : UpdateReceiverNode(capacity, responsibleServer, runningSoftware, updateRetrievalParams, links) {
    private val receiverRegistry = mutableMapOf<UpdateReceiverNode, MutableList<SoftwareState>>()

    // servers have additionally to running Software a registry for all received updates which are
    // their clients (updateRecieverNodes) listening to
    private var updateRegistry: MutableMap<Software, MutableList<SoftwareUpdate>>? = null

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
        Simulator.getUpdateMetrics()?.onArriveAtServer(update, this)
        updateUpdateRegistry(update)
        initUpdatePackages()
    }

    override fun listeningFor(): List<SoftwareState> {
        val receiverNodeListeningFor = super.listeningFor()
        val serverNodeListeningFor = getCurrentUpdateRegistryStates()
        return receiverNodeListeningFor + serverNodeListeningFor
    }

    private fun processRequest(request: UpdateRequest) {
        if (request is PullLatestUpdatesRequest) {
            initUpdatePackageAndPassToLink(request.initialPosition, request.softwareStates)
        } else if (request is RegisterForUpdatesRequest) {
            registerNodeInLocalRegistry(request.initialPosition, request.softwareStates)
            initUpdatePackageAndPassToLink(request.initialPosition, request.softwareStates)
        }
    }

    private fun initUpdatePackages() {
        receiverRegistry.forEach {
            initUpdatePackageAndPassToLink(it.key, it.value)
        }
    }

    fun getInUpdateRegistry(updatableType: Software): List<SoftwareUpdate>? {
        return updateRegistry?.get(updatableType)
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

    // TODO: make listeningFor non nullable
    private fun initUpdatePackageAndPassToLink(
        target: UpdateReceiverNode, states: List<SoftwareState>
    ) {
        states.map {
            getInUpdateRegistry(it.type)?.filter { update ->
                it.type.updateCompatible(
                    it.versionNumber, update.updatesToVersion
                )
            }?.maxBy { compatibleUpdate -> compatibleUpdate.updatesToVersion }
        }?.forEach {
            if (it != null) {
                this.getLinkTo(target)?.lineUpPackage(
                    UpdatePackage(this, target, it!!.size, it)
                )
            }
        }
    }

    private fun registerNodeInLocalRegistry(
        node: UpdateReceiverNode, listeningFor: List<SoftwareState>?
    ) {
        if (listeningFor != null) {
            receiverRegistry[node] = listeningFor.toMutableList()
        } else {
            receiverRegistry[node] = mutableListOf()
        }
    }
}

interface UnreliableElement {
    var online: Boolean

    fun goOnline()
    fun goOffline()
}

class Edge(
    maxElements: Int,
    responsibleUpdateServer: List<Server>,
    runningSoftware: List<SoftwareState>,
    updateRetrievalParams: UpdateRetrievalParams,
    links: MutableList<UnidirectionalLink> = mutableListOf(),
    override var online: Boolean = true,
) : UpdateReceiverNode(maxElements, responsibleUpdateServer, runningSoftware, updateRetrievalParams, links),
    UnreliableElement {

    override fun processUpdate(update: SoftwareUpdate) {
        super.processUpdate(update)
        Simulator.getUpdateMetrics()?.onArriveAtEdge(update, this)
    }

    override fun goOnline() {
        if (online) {
            return
        } else {
            online = true
            onConnected()
        }
    }

    override fun goOffline() {
        online = false
        onDisconnected()
    }
}
