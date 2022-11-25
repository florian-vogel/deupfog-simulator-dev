// TODO: think about bidirectional links -> Link Interface needed
// or just as two unidirectional links
class UnidirectionalLink(
    val from: Node, val to: Node,
) {
    private var getNextPackage: (UnidirectionalLink) -> Package? = { _ -> null }
    private var currentTransmission: Transmission? = null

    init {
        from.addLink(this)
    }

    fun transmissionFinished() {
        if (currentTransmission != null) {
            from.removePackage(currentTransmission!!.p)
            currentTransmission = null
            val nextPackage = getNextPackage(this)
            if (nextPackage != null) {
                tryTransmission(nextPackage)
            }
        }
    }

    fun tryTransmission(nextPackage: Package) {
        if (currentTransmission == null) {
            // TODO: calculate transmission time
            currentTransmission = SimpleTransmission(nextPackage, 10, this)
        }
    }

    fun setGetNextPackage(getNext: (UnidirectionalLink) -> Package?) {
        getNextPackage = getNext
    }
}