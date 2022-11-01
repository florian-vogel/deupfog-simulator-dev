import java.util.*

open class UnidirectionalLink(
    private val destination: Node,
    val queue: LinkedList<Package>,
    var occupiedBy: Package?
) {
    fun getDestination(): Node {
        return destination
    }

    fun isPackageWaiting(): Boolean {
        return queue.isNotEmpty()
    }

    fun getNextPackage(): Package? {
        return queue.peek();
    }

    open fun lineUpPackage(p:Package){
        queue.add(p)
    }

    open fun removeFirst(){
        queue.remove()
    }

    open fun tryTransfer() {}
}

class UnidirectionalLinkPush(destination: Node, queue: LinkedList<Package>) : UnidirectionalLink(destination, queue, null) {
    override fun tryTransfer() {
        if (occupiedBy === null &&  isPackageWaiting()) {
            val nextPackage = getNextPackage()!!
            transferPackage(nextPackage)
        }
    }

    private fun transferPackage(p: Package) {
        occupiedBy = p
        val transmissionTime = 10
        Simulator.addCallback(
            PackageArriveCallback(
                Simulator.getCurrentTimestamp() + transmissionTime,
                PackageArriveCallbackParams(p, this)
            )
        )
    }

    override fun lineUpPackage(p: Package) {
        queue.add(p)
        tryTransfer()
    }

    override fun removeFirst() {
        if (occupiedBy == queue.first()){
            occupiedBy = null
        }
        queue.poll()
        tryTransfer()
    }
}