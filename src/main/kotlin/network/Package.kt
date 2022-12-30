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
    initialPosition: UpdateReceiverNode, destination: Node, packageOverhead: Int, val softwareStates: List<SoftwareState>
) : Package(initialPosition, destination, packageOverhead)


class PullLatestUpdatesRequest(
    packageOverhead: Int,
    initialPosition: UpdateReceiverNode,
    destination: Server,
    softwareStates: List<SoftwareState>,
) : UpdateRequest(initialPosition, destination, packageOverhead, softwareStates)

class RegisterForUpdatesRequest(
    packageOverhead: Int,
    initialPosition: UpdateReceiverNode,
    destination: Server,
    softwareStates: List<SoftwareState>,
) : UpdateRequest(initialPosition, destination, packageOverhead, softwareStates)

class UpdatePackage(
    initialPosition: Node, destination: Node, private val packageOverhead: Int, val update: SoftwareUpdate
) : Package(initialPosition, destination, packageOverhead) {
    override fun getSize(): Int {
        return packageOverhead + update.size
    }
}
