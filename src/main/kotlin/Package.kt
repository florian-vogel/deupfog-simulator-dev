interface Package {
    val initialPosition: Node
    val destination: Node
    val size: Int
}

interface UpdateRequest : Package {
    val softwareStates: List<SoftwareState>
}

data class PullLatestUpdatesRequest(
    override val size: Int,
    override val initialPosition: UpdateReceiverNode,
    override val destination: Server,
    override val softwareStates: List<SoftwareState>,
) : UpdateRequest

data class RegisterForUpdatesRequest(
    override val size: Int,
    override val initialPosition: UpdateReceiverNode,
    override val destination: Server,
    override val softwareStates: List<SoftwareState>,
) : UpdateRequest

data class UpdatePackage(
    override val initialPosition: Node,
    override val destination: Node,
    override val size: Int,
    val update: SoftwareUpdate
) : Package
