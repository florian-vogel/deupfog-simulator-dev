import java.util.*

data class UnidirectionalLinkPush(
    val linksTo: Node,
) {
    var occupiedWith: Package? = null

    // TODO: hier die responsibilities mit Node klären -> wer baut callback, wer bestimmt, wann transfer geklapt hat und wann incht ..
    fun transferPackage(e: Package) {
        if (this.isFree()) {
            occupiedWith = e
            createPackageMoveCallback(e)
        }
    }

    fun completeTransfer() {
        occupiedWith = null
    }

    fun isFree(): Boolean {
        return occupiedWith === null
    }

    private fun createPackageMoveCallback(p: Package) {
        val transmissionTime = 10
        Simulator.callbacks.add(
            MovePackageCallback(
                Simulator.currentState.time + transmissionTime,
                MovePackageCallbackParams(p, p.getPosition(), linksTo)
            )
        )
    }
}
