import Software.SoftwareState
import Software.SoftwareUpdate
import Software.applyUpdates

abstract class Package(
    val initialPosition: Node, val destination: Node, private val size: Int
) {
    open fun getSize(): Int {
        return size
    }
}


abstract class UpdateRequest(
    initialPosition: UpdateReceiverNode, destination: Node, size: Int, val softwareStates: List<SoftwareState>
) : Package(initialPosition, destination, size)


class PullLatestUpdatesRequest(
    size: Int,
    initialPosition: UpdateReceiverNode,
    destination: Server,
    softwareStates: List<SoftwareState>,
) : UpdateRequest(initialPosition, destination, size, softwareStates)

class RegisterForUpdatesRequest(
    size: Int,
    initialPosition: UpdateReceiverNode,
    destination: Server,
    softwareStates: List<SoftwareState>,
) : UpdateRequest(initialPosition, destination, size, softwareStates)

class UpdatePackage(
    initialPosition: Node, destination: Node, private val size: Int, val update: SoftwareUpdate
) : Package(initialPosition, destination, size) {
    override fun getSize(): Int {
        return size + update.size
    }
}
