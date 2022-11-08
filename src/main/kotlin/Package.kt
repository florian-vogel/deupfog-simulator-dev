open class PackagePayload(open val size: Int)

open class Package(
    private val initialPosition: Node, val payload: PackagePayload, val name: String
) {
    open var currentPosition: Node = initialPosition

    open fun setPosition(newPosition: Node) {
        currentPosition = newPosition
    }

    fun getPosition(): Node {
        return currentPosition
    }

    fun getInitialPosition(): Node {
        return initialPosition;
    }
}

class RequestPackage(
    initialPosition: Node, packagePayload: PackagePayload
) : Package(initialPosition, packagePayload, name = "not specified") {

    /* override fun setPosition(newPosition: Node) {
        super.setPosition(newPosition)
        // TODO: hier nochmal überlegen, wie pull requests den pull mechanismus auslösen
    } */
}

class UpdatePackage(
    initialPosition: Node, private val update: SoftwareVersion, name: String
) : Package(
    initialPosition, update, name
) {
    init {
        Simulator.metrics.updateMetricsCollector.registerUpdate(this.update)
    }

    fun getUpdate(): SoftwareVersion {
        return update
    }
}
