import javax.print.attribute.standard.Destination

interface TimedCallback {
    val atInstant: Int
    fun runCallback() {}
}

open class PackageStateChangeCallback(
    override val atInstant: Int, open val p: Package
) : TimedCallback

class PackageArriveCallback(
    override val atInstant: Int, override val p: Package, private val via: UnidirectionalLink
) : PackageStateChangeCallback(atInstant, p) {

    override fun runCallback() {
        movePackage(
            p, p.getPosition(), via.getDestination(), via
        )
    }
}

open class PackageInitializationCallback(
    override val atInstant: Int, open val initialPosition: Node, open val payload: PackagePayload
) : TimedCallback

open class InitPackageCallback(
    override val atInstant: Int,
    override val initialPosition: Node,
    override val payload: PackagePayload,
    val name: String
) : PackageInitializationCallback(atInstant, initialPosition, payload) {

    override fun runCallback() {
        val newPackage = Package(initialPosition, payload, name)
        addPackageAtInitialPosition(newPackage)
    }
}

class InitUpdatePackageCallback(
    atInstant: Int, initialPosition: Node, override val payload: SoftwareVersion, name: String

) : InitPackageCallback(atInstant, initialPosition, payload, name) {
    override fun runCallback() {
        val updatePackage = UpdatePackage(initialPosition, payload, name)
        addPackageAtInitialPosition(updatePackage)
    }
}

class PullRequestSchedulerCallback(
    override val atInstant: Int,
    override val initialPosition: Node,
    val destination: Node,
    override val payload: PackagePayload,
    private val repeatInterval: Int?
) : PackageInitializationCallback(atInstant, initialPosition, payload) {
    override fun runCallback() {
        startPullRequestSchedule(initialPosition, destination, payload, repeatInterval)
    }
}

fun movePackage(p: Package, from: Node, to: Node, via: UnidirectionalLink) {
    from.arrivedVia(via)
    p.setPosition(to)
    to.receive(p)
}

fun addPackageAtInitialPosition(p: Package) {
    p.getInitialPosition().receive(p)
}

fun startPullRequestSchedule(initialPosition: Node, destination: Node, payload: PackagePayload, repeatInterval: Int?) {
    val newRequestPackage = RequestPackage(initialPosition, PackagePayload(0))
    addPackageAtInitialPosition(newRequestPackage)
    if (repeatInterval !== null) {
        Simulator.addCallback(
            PullRequestSchedulerCallback(
                Simulator.getCurrentTimestamp() + repeatInterval, initialPosition, destination, payload, repeatInterval
            )
        )
    }
}
