class Package(initialPosition: Node, private val destination: Node, val size: Int) {
    private var currentPosition: Node = initialPosition
    fun setPosition(newPosition: Node) {
        currentPosition = newPosition
    }

    fun getPosition(): Node {
        return currentPosition
    }

    fun getDestination(): Node {
        return destination
    }
}

