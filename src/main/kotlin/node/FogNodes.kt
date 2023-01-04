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

class PackagesConfigServer(
    registerRequestOverhead: Int,
    pullRequestOverhead: Int,
    val updatePackageOverhead: Int,
    calculatePackageSendProcessingTime: ((p: Package, numOfPackagesInQueue: Int) -> Int) = { _, _ -> 0 },
) : PackagesConfig(registerRequestOverhead, pullRequestOverhead, calculatePackageSendProcessingTime)

open class Server(
    nodeSimParams: NodeConfig,
    responsibleServer: List<Server>,
    runningSoftware: List<SoftwareState>,
    updateRetrievalParams: UpdateRetrievalParams,
    initialServerState: MutableNodeState,
    private val packagesConfig: PackagesConfigServer
) : UpdateReceiverNode(
    nodeSimParams, responsibleServer, runningSoftware, updateRetrievalParams, initialServerState, packagesConfig
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
        // todo: refactor
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
            updatePullNodesRegistry(request.initialPosition, request.softwareStates.toMutableList())
        } else if (request is RegisterForUpdatesRequest) {
            updateSubscriberRegistry(request.initialPosition, request.softwareStates.toMutableList())
        }
    }

    private fun updatePullNodesRegistry(pullNode: UpdateReceiverNode, listeningFor: MutableList<SoftwareState>) {
        val latestValue = recentPullNodesRegistry[pullNode]
        if (latestValue == null || !latestValue.containsAll(listeningFor)) {
            recentPullNodesRegistry[pullNode] = listeningFor
            registerAtServers(listeningFor())
        }
    }

    private fun updateSubscriberRegistry(subscriber: UpdateReceiverNode, listeningFor: MutableList<SoftwareState>) {
        val latestValue = subscriberRegistry[subscriber]
        val latestListeningFor = listeningFor()
        var listeningForContainsNewValue = false
        listeningFor.forEach {
            val listeningForDoesDotCoverValue =
                latestListeningFor.find { state -> state.type == it.type && state.versionNumber == it.versionNumber } == null
            if (listeningForDoesDotCoverValue) {
                listeningForContainsNewValue = true
            }
        }
        if (latestValue == null || listeningForContainsNewValue) {
            subscriberRegistry[subscriber] =
                listeningFor.map { SoftwareState(it.type, it.versionNumber, it.size) }.toMutableList()
            registerAtServers(listeningFor())
        }
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
                val updatePackage = UpdatePackage(this, target, packagesConfig.updatePackageOverhead, it)
                initPackage(updatePackage)
            }
        }
    }

    fun initializeUpdate(updatePackage: UpdatePackage) {
        processUpdate(updatePackage.update)
    }
}

class Edge(
    nodeSimParams: NodeConfig,
    responsibleUpdateServer: List<Server>,
    runningSoftware: List<SoftwareState>,
    updateRetrievalParams: UpdateRetrievalParams,
    initialNodeState: MutableNodeState,
    packagesConfig: PackagesConfig
) : UpdateReceiverNode(
    nodeSimParams, responsibleUpdateServer, runningSoftware, updateRetrievalParams, initialNodeState, packagesConfig
) {}
