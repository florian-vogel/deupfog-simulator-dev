import java.util.*

// TODO: think about bidirectional links -> Link Interface needed
class UnidirectionalLink(
    val from: Node,
    val to: Node,
    private val queue: LinkedList<Package> = LinkedList(),
) {
    // TODO: allow multiple transmissions at the same time, instead track used bandwith
    private var currentTransmission: Transmission? = null

    fun lineUpPackage(p: Package) {
        queue.add(p)
    }

    fun elementsWaiting(): Int {
        return queue.size
    }

    fun transmissionFinished() {
        currentTransmission = null
    }

    fun tryTransmission() {
        if (isFree() && queue.isNotEmpty()) {
            val nextPackage = queue.remove()
            currentTransmission = SimpleTransmission(nextPackage, 10, this)
        }
    }

    private fun isFree(): Boolean {
        return currentTransmission == null
    }
}