import java.util.*
import kotlin.collections.HashMap
import kotlin.streams.toList

class Network(val nodes: LinkedList<Node>) {
    fun findShortestPath(from: Node, to: Node): LinkedList<Node> {
        // TODO: this is a dummy implementation, does not work if one element has more than one link
        val shortestPath = LinkedList<Node>()
        val linksAtNode = from.getLinks()
        if (linksAtNode.isNotEmpty()) {
            shortestPath.add(linksAtNode.first().linksTo)
        }
        return shortestPath
    }
}

class Node(val links: LinkedList<UnidirectionalLinkPush>, private val maxElements: Int) {
    private val queues: Map<UnidirectionalLinkPush, PriorityQueue<Package>> =
        links.associateWith { PriorityQueue<Package>() }

    fun add(p: Package, nextHop: Node?) {
        if (p.destination === this) {
            println("package arrived $p")
            return
        } else if (nextHop === null) {
            println("no connection to package destination $p")
        } else if (elementsAtNode() < maxElements) {
            val linkToNextHop = getLinkTo(nextHop)
            passPackageToLink(p, linkToNextHop!!)
        }
    }

    private fun passPackageToLink(p: Package, link: UnidirectionalLinkPush) {
        if (link.isFree()) {
            link.startTransfer(p)
        } else {
            queues[link]?.add(p)
        }
    }


    fun addLink(link: UnidirectionalLinkPush) {
        this.links.add(link)
    }

    fun getLinks(): List<UnidirectionalLinkPush> {

        return this.links

    }

    fun getLinkTo(node: Node): UnidirectionalLinkPush? {
        return links.find { it.linksTo === node }
    }

    fun remove(p: Package) {
    }

    private fun elementsAtNode(): Int {
        return this.queues.values.sumOf { it.size }
    }
}