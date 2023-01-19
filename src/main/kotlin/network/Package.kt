package network

import node.Node
import node.Server
import node.UpdateReceiverNode
import software.SoftwareState
import software.SoftwareUpdate

abstract class Package(
    val initialPosition: Node, val destination: Node, private val packageOverhead: Int
) {
    open fun getSize(): Int {
        return packageOverhead
    }
}

abstract class UpdateRequest(
    initialPosition: UpdateReceiverNode,
    destination: Node,
    packageOverhead: Int,
    val softwareInformation: SoftwareInformation
) : Package(initialPosition, destination, packageOverhead)


class PullLatestUpdatesRequest(
    packageOverhead: Int,
    initialPosition: UpdateReceiverNode,
    destination: Server,
    softwareInformation: SoftwareInformation,
) : UpdateRequest(initialPosition, destination, packageOverhead, softwareInformation)

class RegisterForUpdatesRequest(
    packageOverhead: Int,
    initialPosition: UpdateReceiverNode,
    destination: Server,
    softwareInformation: SoftwareInformation,
) : UpdateRequest(initialPosition, destination, packageOverhead, softwareInformation)

class UpdatePackage(
    initialPosition: Node, destination: Node, private val packageOverhead: Int, val update: SoftwareUpdate
) : Package(initialPosition, destination, packageOverhead) {
    override fun getSize(): Int {
        return packageOverhead + update.size
    }
}

open class SoftwareInformation(private val runningSoftware: List<SoftwareState>) {
    open fun updateNeeded(update: SoftwareUpdate): Boolean {
        return runningSoftware.find { it.type == update.type && it.type.updateCompatible(it, update) } != null
    }

    open fun containsAllInformationOf(softwareInformation: SoftwareInformation): Boolean {
        return softwareInformation.runningSoftware.find { runningSoftware.find { state -> it.isEqual(state) } == null } == null
    }
}

class ServerSoftwareInformation(
    runningSoftware: List<SoftwareState>,
    private val knownUpdates: List<SoftwareUpdate>,
    private val listenerSoftwareInformation: List<SoftwareInformation>,
) :
    SoftwareInformation(runningSoftware) {

    // todo: rename
    override fun updateNeeded(update: SoftwareUpdate): Boolean {
        val updateUnknown =
            knownUpdates.find { it.type == update.type && it.updatesToVersion == update.updatesToVersion } == null
        val listenerNeedsUpdate = listenerSoftwareInformation.find { it.updateNeeded(update) } != null
        return super.updateNeeded(update) || (updateUnknown && listenerNeedsUpdate)
    }

    override fun containsAllInformationOf(softwareInformation: SoftwareInformation): Boolean {
        if (softwareInformation is ServerSoftwareInformation) {
            val containsAllRunningSoftwareInformation = super.containsAllInformationOf(softwareInformation)
            val containsAllUpdateInformation = knownUpdates.containsAll(softwareInformation.knownUpdates)
            val containsAllListenerInformation = softwareInformation.listenerSoftwareInformation.all {
                listenerSoftwareInformation.find { info ->
                    info.containsAllInformationOf(it)
                } != null
            }
            return containsAllRunningSoftwareInformation && containsAllUpdateInformation && containsAllListenerInformation
        } else {
            return false
        }
    }
}