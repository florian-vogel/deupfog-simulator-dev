open class Package(private val initialPosition: Node, private val destination: Node, val size: Int) {
    open var currentPosition: Node = initialPosition
    open fun setPosition(newPosition: Node) {
        currentPosition = newPosition
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

class RequestPackage(initialPosition: Node, destination: Node, size: Int) :
    Package(initialPosition, destination, size) {

    override fun setPosition(newPosition: Node) {
        if (newPosition === getDestination()) {
            getDestination().getLinkTo(this.getInitialPosition())!!.tryTransfer()
        }
        currentPosition = newPosition
    }
}
