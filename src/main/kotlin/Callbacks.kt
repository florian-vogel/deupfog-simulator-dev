interface TimedCallback {
    val atInstant: Int
    fun runCallback() {}
}

open class PackageStateChangeCallback(
    override val atInstant: Int,
    open val p: Package
) : TimedCallback

class PackageArriveCallback(
    override val atInstant: Int,
    override val p: Package,
    private val via: UnidirectionalLink
) : PackageStateChangeCallback(atInstant, p) {

    override fun runCallback() {
        movePackage(
            p,
            p.getPosition(),
            via.getDestination(),
            via
        )
    }
}

open class PackageInitializationCallback(
    override val atInstant: Int,
    open val initialPosition: Node,
    open val destination: Node,
    open val size: Int
) : TimedCallback

data class InitPackageCallback(
    override val atInstant: Int,
    override val initialPosition: Node,
    override val destination: Node,
    override val size: Int,
    val name: String
) : PackageInitializationCallback(atInstant, initialPosition, destination, size) {

    override fun runCallback() {
        val newPackage = Package(initialPosition, destination, size, name)
        addPackageAtInitialPosition(newPackage)
    }
}

class PullRequestSchedulerCallback(
    override val atInstant: Int,
    override val initialPosition: Node,
    override val destination: Node,
    override val size: Int,
    private val repeatInterval: Int?
) :
    PackageInitializationCallback(atInstant, initialPosition, destination, size) {
    override fun runCallback() {
        startPullRequestSchedule(initialPosition, destination, size, repeatInterval)
    }
}

fun movePackage(p: Package, from: Node, to: Node, via: UnidirectionalLink) {
    from.arrivedVia(via)
    p.setPosition(to)
    val nextHop = Simulator.findNextHop(p)
    to.receive(p, nextHop)
}

fun addPackageAtInitialPosition(p: Package) {
    val nextHop = Simulator.findNextHop(p)
    p.getInitialPosition().receive(p, nextHop)
}

fun startPullRequestSchedule(initialPosition: Node, destination: Node, size: Int, repeatInterval: Int?) {
    val newRequestPackage = RequestPackage(initialPosition, destination, size)
    addPackageAtInitialPosition(newRequestPackage)
    if (repeatInterval !== null) {
        Simulator.addCallback(
            PullRequestSchedulerCallback(
                Simulator.getCurrentTimestamp() + repeatInterval,
                initialPosition, destination, size, repeatInterval
            )
        )
    }
}
