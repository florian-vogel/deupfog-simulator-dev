open class Package(
    private val initialPosition: Node,
    private val destination: Node,
    val size: Int,
    val name: String
) {
    open var currentPosition: Node = initialPosition

    init {
        Simulator.metrics.packageMetricsCollector.register(this)
    }

    open fun setPosition(newPosition: Node) {
        currentPosition = newPosition
        if (newPosition === getDestination()) {
            Simulator.metrics.packageMetricsCollector.getMetricsCollector(this)?.onArrive()
        }
    }

    fun getPosition(): Node {
        return currentPosition
    }

    fun getDestination(): Node {
        return destination
    }

    fun getInitialPosition(): Node {
        return initialPosition;
    }
}

class RequestPackage(
    initialPosition: Node,
    destination: Node,
    size: Int
) :
    Package(initialPosition, destination, size, name = "not specified") {

    override fun setPosition(newPosition: Node) {
        super.setPosition(newPosition)
        if (newPosition === getDestination()) {
            getDestination().getLinkTo(this.getInitialPosition())!!.tryTransfer()
        }
    }
}
