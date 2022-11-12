import java.util.*

open class Node(
    private val links: List<UnidirectionalLink>, private val capacity: Int,
) {

    open fun receive(p: Package) {
        // TODO maybe make payload only accessible by destination node
        if (!arrivedAtDestination(p, this)) {
            forward(p)
            return
        }
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

abstract class UpdateReceiverNode<TUpdatable : UpdatableType>(
    links: List<UnidirectionalLink>,
    capacity: Int,
    private val responsibleServer: List<Server<TUpdatable>>,
    private val listeningFor: List<TUpdatable>,
    private val updateRetrievalParams: UpdateRetrievalParams,
) : Node(links, capacity) {

    private val updateRegistry = mutableMapOf<TUpdatable, MutableList<UpdatableUpdate<TUpdatable>>>()
    private var pullRequestSchedule: TimedCallback? = null

    init {
        onConnected()
    }

    override fun receive(p: Package) {
        super.receive(p)
        if (p is UpdateResponse<*>) {
            processUpdate(p as UpdateResponse<TUpdatable>)
        }
    }

    open fun processUpdate(request: UpdateResponse<TUpdatable>) {
        updateUpdateRegistry(request.update)
    }

    fun onConnected() {
        if (updateRetrievalParams.registerAtServerForUpdates) {
            registerAtServer()
            sendPullRequest()

        }
        if (updateRetrievalParams.sendUpdateRequestsInterval !== null) {
            initPullRequestSchedule()
        }
    }

    fun onDisconnected() {
        removePullRequestSchedule()
    }

    fun getInUpdateRegistry(updatableType: UpdatableType): List<UpdatableUpdate<TUpdatable>>? {
        return updateRegistry[updatableType]
    }

    private fun updateUpdateRegistry(update: UpdatableUpdate<TUpdatable>) {
        if (updateRegistry[update.type] == null) {
            updateRegistry[update.type] = mutableListOf(update)
        } else {
            updateRegistry[update.type]!!.add(update)
        }
    }

    private fun registerAtServer() {
        responsibleServer.forEach {
            val request = RegisterForUpdatesRequest(1, this, it, getCurrentUpdatableStates())
            val nextHop = findShortestPath(this, it)?.peek()
            if (nextHop != null) {
                getLinkTo(nextHop)?.lineUpPackage(request)
            }
        }
    }

    private fun sendPullRequest() {
        responsibleServer.forEach {
            val request = PullLatestUpdatesRequest(1, this, it, getCurrentUpdatableStates())
            val nextHop = findShortestPath(this, it)?.peek()
            if (nextHop != null) {
                getLinkTo(nextHop)?.lineUpPackage(request)
            }
        }
    }

    private fun getCurrentUpdatableStates(): List<UpdatableState<TUpdatable>> {
        val states = mutableListOf<UpdatableState<TUpdatable>>()
        updateRegistry.forEach {
            val state = UpdatableState(it.key)
            it.value.sortBy { update -> update.updatesToVersion }
            it.value.forEach { update -> state.applyUpdate(update) }
            states.add(state)
        }
        return states
    }

    private fun initPullRequestSchedule() {
        // TODO
    }

    private fun removePullRequestSchedule() {
        pullRequestSchedule = null
    }

}

open class Server<TUpdatable : UpdatableType>(
    links: List<UnidirectionalLink>,
    capacity: Int,
    responsibleServer: List<Server<TUpdatable>>,
    listeningFor: List<TUpdatable>,
    updateRetrievalParams: UpdateRetrievalParams
) : UpdateReceiverNode<TUpdatable>(links, capacity, responsibleServer, listeningFor, updateRetrievalParams) {
    private val receiverRegistry =
        mutableMapOf<UpdateReceiverNode<TUpdatable>, MutableList<UpdatableState<TUpdatable>>>()

    override fun receive(p: Package) {
        super.receive(p)
        // TODO: work with reified and inline functions
        if (p is UpdateRequest<*>) {
            processRequest(p as UpdateRequest<TUpdatable>)
        }
    }

    override fun processUpdate(request: UpdateResponse<TUpdatable>) {
        super.processUpdate(request)
        initUpdatePackages()
    }

    private fun processRequest(request: UpdateRequest<TUpdatable>) {
        if (request is PullLatestUpdatesRequest<TUpdatable>) {
            initUpdatePackageAndPassToLink(request.initialPosition, request.requesterUpdatables)
        } else if (request is RegisterForUpdatesRequest<TUpdatable>) {
            registerNodeInLocalRegistry(request.initialPosition, request.requesterUpdatables)
            initUpdatePackageAndPassToLink(request.initialPosition, request.requesterUpdatables)
        }
    }

    private fun initUpdatePackages() {
        receiverRegistry.forEach {
            initUpdatePackageAndPassToLink(it.key, it.value)
        }
    }

    // TODO: make listeningFor non nullable
    private fun initUpdatePackageAndPassToLink(
        target: UpdateReceiverNode<TUpdatable>, states: List<UpdatableState<TUpdatable>>
    ) {
        states.map {
            getInUpdateRegistry(it.type)?.filter { update ->
                it.type.updateCompatible(
                    it.versionNumber, update.updatesToVersion
                )
            }?.maxBy { compatibleUpdate -> compatibleUpdate.updatesToVersion }
        }?.forEach {
            this.getLinkTo(target)?.lineUpPackage(
                UpdateResponse(this, target, it!!.size, it)
            )
        }
    }

    private fun registerNodeInLocalRegistry(
        node: UpdateReceiverNode<TUpdatable>, software: List<UpdatableState<TUpdatable>>?
    ) {
        if (software != null) {
            receiverRegistry[node] = software.toMutableList()
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

class Edge<TUpdatable : UpdatableType>(
    links: LinkedList<UnidirectionalLink>,
    maxElements: Int,
    responsibleUpdateServer: List<Server<TUpdatable>>,
    listeningFor: List<TUpdatable>,
    updateRetrievalParams: UpdateRetrievalParams,
    override var online: Boolean = true,
) : UpdateReceiverNode<TUpdatable>(links, maxElements, responsibleUpdateServer, listeningFor, updateRetrievalParams),
    UnreliableElement {

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
