package node

import network.*
import software.Software
import software.SoftwareState
import software.SoftwareUpdate

class PackagesConfigServer(
    registerRequestOverhead: Int,
    pullRequestOverhead: Int,
    val updatePackageOverhead: Int,
    calculatePackageSendProcessingTime: ((p: Package, numOfPackagesInQueue: Int) -> Int) = { _, _ -> 0 },
) : PackagesConfig(registerRequestOverhead, pullRequestOverhead, calculatePackageSendProcessingTime)

open class Server(
    nodeSimParams: NodeConfig,
    responsibleServer: List<Server>,
    runningSoftware: MutableList<SoftwareState>,
    updateRetrievalParams: UpdateRetrievalParams,
    initialServerState: MutableNodeState,
    private val packagesConfig: PackagesConfigServer
) : UpdateReceiverNode(
    nodeSimParams, responsibleServer, runningSoftware, updateRetrievalParams, initialServerState, packagesConfig
) {
    private val subscriberRegistry: MutableMap<UpdateReceiverNode, SoftwareInformation> = mutableMapOf()
    private val recentPullNodesRegistry: MutableMap<UpdateReceiverNode, SoftwareInformation> = mutableMapOf()
    private val updateRegistry: MutableMap<Software, MutableList<SoftwareUpdate>> = mutableMapOf()

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
        val updateAlreadyInRegistry = getCurrentUpdateRegistryValues().contains(update)
        if (updateAlreadyInRegistry) {
            return;
        } else {
            updateUpdateRegistry(update)
            initUpdatePackagesToAllSubscribers(listOf(update))
        }
    }

    override fun softwareInformation(): SoftwareInformation {
        val listenerSoftwareInformation =
            (getCurrentSubscriberRegistrySoftwareInformation() + getCurrentRecentPullNodesRegistrySoftwareInformation())
        val serverUpdates = getCurrentUpdateRegistryValues()
        val runningSoftwareCopy = runningSoftware.toList()
        return ServerSoftwareInformation(runningSoftwareCopy, serverUpdates, listenerSoftwareInformation)
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
        initUpdatePackageAndPassToLink(request.initialPosition as UpdateReceiverNode, request.softwareInformation)
        if (request is PullLatestUpdatesRequest) {
            updatePullNodesRegistry(request.initialPosition, request.softwareInformation)
        } else if (request is RegisterForUpdatesRequest) {
            updateSubscriberRegistry(request.initialPosition, request.softwareInformation)
        }
    }

    private fun updatePullNodesRegistry(pullNode: UpdateReceiverNode, softwareInformation: SoftwareInformation) {
        val currentPullNodeSoftwareInformation = recentPullNodesRegistry[pullNode]
        if (currentPullNodeSoftwareInformation == null ||
            !currentPullNodeSoftwareInformation.containsAllInformationOf(softwareInformation)
        ) {
            recentPullNodesRegistry[pullNode] = softwareInformation
            registerAtServers(softwareInformation())
        }
    }

    private fun updateSubscriberRegistry(subscriber: UpdateReceiverNode, softwareInformation: SoftwareInformation) {
        val currentSubscriberSoftwareInformation = subscriberRegistry[subscriber]
        if (currentSubscriberSoftwareInformation == null ||
            !currentSubscriberSoftwareInformation.containsAllInformationOf(softwareInformation)
        ) {
            subscriberRegistry[subscriber] = softwareInformation
            registerAtServers(softwareInformation())
        }
    }

    private fun initUpdatePackagesToAllSubscribers(newUpdates: List<SoftwareUpdate>) {
        subscriberRegistry.forEach {
            initUpdatePackageAndPassToLink(it.key, it.value, newUpdates)
        }
    }

    private fun updateUpdateRegistry(update: SoftwareUpdate) {
        val registry = updateRegistry[update.type];
        if (registry == null) {
            updateRegistry[update.type] = mutableListOf(update)
        } else {
            registry.add(update)
        }
    }

    private fun getCurrentUpdateRegistryValues(): List<SoftwareUpdate> {
        return updateRegistry.values.flatten()
    }

    private fun getCurrentRecentPullNodesRegistrySoftwareInformation(): List<SoftwareInformation> {
        return recentPullNodesRegistry.map { it.value }
    }

    private fun getCurrentSubscriberRegistrySoftwareInformation(): List<SoftwareInformation> {
        return subscriberRegistry.map { it.value }
    }

    private fun initUpdatePackageAndPassToLink(
        target: UpdateReceiverNode,
        targetSoftwareInformation: SoftwareInformation,
        newUpdates: List<SoftwareUpdate>? = null
    ) {
        val updates = newUpdates ?: getCurrentUpdateRegistryValues()
        //val updates = getCurrentUpdateRegistryValues().filter { newUpdates.contains(it) }
        val updatesNeededByTarget = updates.filter { update ->
            targetSoftwareInformation.updateNeeded(update)
        }.groupBy { it.type }.map { updatesOfType ->
            updatesOfType.value.maxByOrNull { it.updatesToVersion }
        }
        updatesNeededByTarget.forEach {
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
    runningSoftware: MutableList<SoftwareState>,
    updateRetrievalParams: UpdateRetrievalParams,
    initialNodeState: MutableNodeState,
    packagesConfig: PackagesConfig
) : UpdateReceiverNode(
    nodeSimParams, responsibleUpdateServer, runningSoftware, updateRetrievalParams, initialNodeState, packagesConfig
) {}
