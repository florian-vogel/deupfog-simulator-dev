import java.util.*

// TODO: nochmal überlegen auf welche ebene die queue kommt
// TODO: überlegung: unterscheide zwischen updatePackages und requestPackage
// requestPackages werden immer gepushed, aber es könnte weiterhin eine strategie für update packages bestimmt werden
open class UnidirectionalLink(
    val at: Node,
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

    open fun lineUpPackage(p: Package) {
        queue.add(p)
    }

    open fun removeFirst() {
        queue.remove()
    }

    open fun tryTransfer() {}
}

class UnidirectionalLinkPush(at: Node, destination: Node, queue: LinkedList<Package>) :
    UnidirectionalLink(at, destination, queue, null) {

    override fun tryTransfer() {
        if (occupiedBy === null && isPackageWaiting()) {
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
        if (occupiedBy == queue.first()) {
            occupiedBy = null
        }
        queue.poll()
        tryTransfer()
    }
}

class UnidirectionalLinkPull(at: Node, destination: Node, queue: LinkedList<Package>) :
    UnidirectionalLink(at, destination, queue, null) {

    init {
        initRequestSchedule()
    }

    private fun initRequestSchedule() {
        // TODO: two times destination passed
        val requestPackage = RequestPackage(this.getDestination(), this.at, 1)
        Simulator.addCallback(
            InitPackageCallback(
                Simulator.getCurrentTimestamp(),
                InitPackageCallbackParams(requestPackage, 10)
            )
        )
    }

    override fun tryTransfer() {
        if (occupiedBy === null && isPackageWaiting()) {
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
    }

    // TODO: this doesn't seem right
    override fun removeFirst() {
        if (occupiedBy == queue.first()) {
            occupiedBy = null
        }
        queue.poll()
    }
}
