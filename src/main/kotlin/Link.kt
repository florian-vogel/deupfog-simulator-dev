import java.util.*

// TODO: think about bidirectional links -> Link Interface needed
class UnidirectionalLink(
    from: Node,
    val to: Node,
    private val queue: LinkedList<Package> = LinkedList(),
) {
    init {
        from.addLink(this)
    }

    // TODO: allow multiple transmissions at the same time, instead track used bandwith
    private var currentTransmission: Transmission? = null

    fun lineUpPackage(p: Package) {
        queue.add(p)
        tryTransmission()
    }

    fun elementsWaiting(): Int {
        return queue.size
    }

    fun transmissionFinished() {
        currentTransmission = null
        tryTransmission()
    }

    fun tryTransmission() {
        if (isFree() && queue.isNotEmpty()) {
            val nextPackage = queue.remove()
            // TODO: calculate transmission time
            currentTransmission = SimpleTransmission(nextPackage, 10, this)
        }
    }

    private fun isFree(): Boolean {
        return currentTransmission == null
    }
}