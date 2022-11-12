interface Package {
    val initialPosition: Node
    val destination: Node
    val size: Int
}

interface UpdateRequest<TUpdatable : UpdatableType> : Package {
    val requesterUpdatables: List<UpdatableState<TUpdatable>>
}

data class PullLatestUpdatesRequest<TUpdatable : UpdatableType>(
    override val size: Int,
    override val initialPosition: UpdateReceiverNode<TUpdatable>,
    override val destination: Server<TUpdatable>,
    override val requesterUpdatables: List<UpdatableState<TUpdatable>>,
) : UpdateRequest<TUpdatable>

data class RegisterForUpdatesRequest<TUpdatable : UpdatableType>(
    override val size: Int,
    override val initialPosition: UpdateReceiverNode<TUpdatable>,
    override val destination: Server<TUpdatable>,
    override val requesterUpdatables: List<UpdatableState<TUpdatable>>,
) : UpdateRequest<TUpdatable>

data class UpdateResponse<TUpdatable : UpdatableType>(
    override val initialPosition: Node,
    override val destination: Node,
    override val size: Int,
    val update: UpdatableUpdate<TUpdatable>
) : Package
