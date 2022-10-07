import java.util.*
import kotlin.streams.toList

class Network(val nodes: LinkedList<Node>) {
    fun findShortestPath(from: PackagePosition, to: PackagePosition): LinkedList<PackagePosition> {
        // TODO: this is a dummy implementation, does not work if one element has more than one link
        val shortestPath = LinkedList<PackagePosition>()
        if (from.linksTo().isNotEmpty()){
            shortestPath.add(from.linksTo().first())
        }

        return shortestPath
    }
}

open class Node(val linksTo: MutableMap<PackagePosition, UnidirectionalLinkPush>, override val maxElements: Int) :
    PackagePosition {

    override fun add(p: Package, nextHop: PackagePosition?) {
        if (p.destination === this) {
            // package arrived
            println("package arrived $p")
            return
        }
        if (elementsAtNode() < maxElements) {
            val linkToNextHop = linksTo[nextHop]
            if (linkToNextHop !== null) {
                linkToNextHop.addToQueue(p)
            }
        }
    }

    override fun remove(p: Package) {
    }

    override fun linksTo(): List<PackagePosition> {
        return this.linksTo.values.stream().map { it.linksTo }.toList()
    }

    private fun elementsAtNode(): Int {
        var counter = 0
        linksTo.entries.stream().forEach {
            counter += it.value.numberOfElementsInQueue()
        }
        return counter
    }
}

// TODO: this interface is currently only implemented by the node class -> redundant
interface PackagePosition {
    val maxElements: Int?

    fun add(p: Package, nextHop: PackagePosition?) {}
    fun remove(p: Package) {}

    // TODO: might be not needed later
    fun linksTo(): List<PackagePosition>
}

interface PositionablePackage {
    val initialPosition: PackagePosition
    fun setPosition(newPosition: PackagePosition)
    fun getPosition(): PackagePosition
}

