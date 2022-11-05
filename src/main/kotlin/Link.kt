import java.util.*

// TODO: nochmal überlegen auf welche ebene die queue kommt
// TODO: überlegung: unterscheide zwischen updatePackages und requestPackage
// requestPackages werden immer gepushed, aber es könnte weiterhin eine strategie für update packages bestimmt werden
open class UnidirectionalLink(
    val at: Node,
    private val destination: Node,
    val queue: LinkedList<Package>,
) {
    private var transferringTop: Boolean = false

    init {
        Simulator.metrics.linkMetricsCollector.register(this)
    }

    fun getDestination(): Node {
        return destination
    }

    fun isPackageWaiting(): Boolean {
        return queue.isNotEmpty()
    }

    fun getNextPackage(): Package? {
        return queue.peek();
    }

    fun setStateToTransferring() {
        this.transferringTop = true
        Simulator.metrics.linkMetricsCollector.getMetricsCollectorForLink(this)?.onStartTransfer()
    }

    fun isFree(): Boolean {
        return !this.transferringTop
    }

    open fun lineUpPackage(p: Package) {
        queue.add(p)
        Simulator.metrics.packageMetricsCollector.getMetricsCollector(p)?.onAddedToQueue()
    }

    open fun removeFirst() {
        queue.remove()
        transferringTop = false
        Simulator.metrics.linkMetricsCollector.getMetricsCollectorForLink(this)?.onStopTransfer()
    }

    fun tryTransfer() {
        if (isFree() && isPackageWaiting()) {
            val nextPackage = getNextPackage()!!
            Simulator.metrics.packageMetricsCollector.getMetricsCollector(nextPackage)?.onStartTransfer()
            transferPackage(nextPackage)
        }
    }

    private fun transferPackage(p: Package) {
        setStateToTransferring()
        val transmissionTime = 10
        Simulator.addCallback(
            PackageArriveCallback(
                Simulator.getCurrentTimestamp() + transmissionTime,
                p, this
            )
        )
    }
}

class UnidirectionalLinkPush(
    at: Node,
    destination: Node,
    queue: LinkedList<Package>
) :
    UnidirectionalLink(at, destination, queue) {

    override fun lineUpPackage(p: Package) {
        super.lineUpPackage(p)
        tryTransfer()
    }

    override fun removeFirst() {
        super.removeFirst()
        tryTransfer()
    }
}

class UnidirectionalLinkPull(
    at: Node,
    destination: Node,
    queue: LinkedList<Package>
) :
    UnidirectionalLink(at, destination, queue) {

    init {
        initRequestSchedule()
    }

    private fun initRequestSchedule() {
        Simulator.addCallback(
            PullRequestSchedulerCallback(
                Simulator.getCurrentTimestamp(),
                getDestination(), at, 1, 10
            )
        )
    }
}
