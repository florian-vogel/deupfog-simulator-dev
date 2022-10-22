data class UnidirectionalLinkPush(
    private val destination: Node,
) {
    private var occupiedWith: Package? = null

    fun occupyWith(p: Package) {
        occupiedWith = p
    }

    fun resetOccupyWith() {
        occupiedWith = null
    }

    fun isFree(): Boolean {
        return occupiedWith === null
    }

    fun getDestination(): Node {
        return destination
    }
}
