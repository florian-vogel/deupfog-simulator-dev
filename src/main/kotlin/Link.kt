import java.util.*

data class UnidirectionalLinkPush(
    val linksTo: PackagePosition,
) {
    var occupiedWith: Package? = null
    private var queue = LinkedList<Package>()

    fun addToQueue(e: Package) {
        queue.add(e)
        processNextElementInQueue()
    }

    fun popQueue() {
        queue.remove()
        processNextElementInQueue()
    }


    fun processNextElementInQueue() {
        if (occupiedWith === null) {
            val nextPackage = queue.first()
            occupiedWith = nextPackage
            val transmissionTime = 10
            println("create new callback in Link: $this")
            Simulator.callbacks.add(
                MovePackageCallback(
                    Simulator.currentState.time + transmissionTime,
                    MovePackageCallbackParams(nextPackage, nextPackage.getPosition(), linksTo)
                )
            )
        }
    }


    fun numberOfElementsInQueue(): Int {
        return queue.size
    }
}
