class Package(val initialPosition: Node, val destination: Node, val size: Int) {
    private var currentPosition: Node = initialPosition
    fun setPosition(newPosition: Node) {
        currentPosition = newPosition
    }

    fun getPosition(): Node {
        return currentPosition
    }
}

