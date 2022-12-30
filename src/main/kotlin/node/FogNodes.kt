package node

import PullLatestUpdatesRequest
import RegisterForUpdatesRequest
import network.UnidirectionalLink
import UpdatePackage
import UpdateRequest
import software.Software
import software.SoftwareState
import software.SoftwareUpdate
import software.applyUpdates
import Package
import network.LinkConfig
import network.MutableLinkState

open class Server(
    nodeSimParams: NodeSimParams,
    responsibleServer: List<Server>,
    runningSoftware: List<SoftwareState>,
    updateRetrievalParams: UpdateRetrievalParams,
    initialServerState: MutableNodeState
) : UpdateReceiverNode(
    nodeSimParams, responsibleServer, runningSoftware, updateRetrievalParams, initialServerState
) {
    private val subscriberRegistry: MutableMap<UpdateReceiverNode, MutableList<SoftwareState>> = mutableMapOf()
    private val updateRegistry: MutableMap<Software, MutableList<SoftwareUpdate>> = mutableMapOf()
    private val recentPullNodesRegistry: MutableMap<UpdateReceiverNode, MutableList<SoftwareState>> = mutableMapOf()

    override fun receive(p: Package) {
        if (!getOnlineState()) return
        if (p.destination == this) {
            if (p is UpdatePackage) {
                processUpdate(p.update)
            }
            if (p is UpdateRequest) {
                processRequest(p)
            }
        } else {
            addToPackageQueue(p)
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

    override fun createLink(linkConfig: LinkConfig, to: Node, initialLinkState: MutableLinkState): UnidirectionalLink {
        val newLink = UnidirectionalLink(linkConfig, this, to, initialLinkState)
        links.add(newLink)
        return newLink
    }

    override fun removeLink(link: UnidirectionalLink) {
        super.removeLink(link)
        val noOtherLinkTo = links.find { it.to == link.to } == null
        if (noOtherLinkTo) {
            subscriberRegistry.remove(link.to)
        }
    }

    private fun processRequest(request: UpdateRequest) {
        initUpdatePackageAndPassToLink(request.initialPosition as UpdateReceiverNode, request.softwareStates)
        if (request is PullLatestUpdatesRequest) {
            recentPullNodesRegistry[request.initialPosition] = request.softwareStates.toMutableList()
        } else if (request is RegisterForUpdatesRequest) {
            subscriberRegistry[request.initialPosition] = request.softwareStates.toMutableList()
        }
        registerAtServers(listeningFor())
    }

    private fun initUpdatePackagesToAllSubscribers() {
        subscriberRegistry.forEach {
            initUpdatePackageAndPassToLink(it.key, it.value)
        }
    }

    private fun updateUpdateRegistry(update: SoftwareUpdate) {
        val registry = updateRegistry[update.type];
        if (registry == null) {
            // todo: can I replace this with registry = ...
            updateRegistry[update.type] = mutableListOf(update)
        } else {
            registry.add(update)
        }
    }

    private fun getCurrentUpdateRegistryStates(): List<SoftwareState> {
        val updateRegistry = updateRegistry.toList() ?: return emptyList()
        return updateRegistry.map {
            applyUpdates(it.first, it.second)
        }
    }

    private fun getCurrentRecentPullNodesRegistryStates(): List<SoftwareState> {
        // todo: remove, maybe refactor whole registry approach,
        // just save where to send to and what to listenFor
        // don't care about push pull nodes or subscribers
        return recentPullNodesRegistry.map { it.value }.flatten()
    }

    private fun getCurrentSubscriberRegistryStates(): List<SoftwareState> {
        return subscriberRegistry.map { it.value }.flatten()
    }

    private fun initUpdatePackageAndPassToLink(
        target: UpdateReceiverNode, targetSoftwareStates: List<SoftwareState>
    ) {
        targetSoftwareStates.map {
            updateRegistry[it.type]?.filter { update ->
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

    fun initializeUpdate(updatePackage: UpdatePackage) {
        //receive(updatePackage)
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
