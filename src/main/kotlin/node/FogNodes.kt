package node

import PullLatestUpdatesRequest
import RegisterForUpdatesRequest
import UnidirectionalLink
import UpdatePackage
import UpdateRequest
import software.Software
import software.SoftwareState
import software.SoftwareUpdate
import software.applyUpdates
import Package

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

    override fun addLink(link: UnidirectionalLink) {
        super.addLink(link)
        if (link.getOnlineState()) {
            if (link.to is UpdateReceiverNode) {
                val receiverRegistryEntry = subscriberRegistry[link.to]
                if (receiverRegistryEntry != null) {
                    initUpdatePackageAndPassToLink(link.to, receiverRegistryEntry)
                }
            }
        }
    }

    override fun removeLink(link: UnidirectionalLink) {
        super.removeLink(link)

        // todo: check if other links exist
        subscriberRegistry.remove(link.to)
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
        subscriberRegistry.forEach {
            initUpdatePackageAndPassToLink(it.key, it.value)
        }
    }

    private fun updateUpdateRegistry(update: SoftwareUpdate) {
        // todo: updateing update registry might be linked to sendOfUpdatesToSubs
        // show link physically
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

    private fun sendAvailableUpdatesAndRegisterNodeInLocalRegistry(
        node: UpdateReceiverNode, listeningFor: List<SoftwareState>, registerAsSubscriber: Boolean
    ) {
        if (registerAsSubscriber) {
            subscriberRegistry[node] = listeningFor.toMutableList()
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
        receive(updatePackage)
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
