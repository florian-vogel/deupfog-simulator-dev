import java.util.*

class Network() {
    fun findShortestPath(from: Node, to: Node): LinkedList<Node> {
        // TODO: this is a dummy implementation, does not work if one element has more than one link
        val shortestPath = LinkedList<Node>()
        val linksAtNode = from.getLinks()
        if (linksAtNode.isNotEmpty()) {
            shortestPath.add(linksAtNode.first().getDestination())
        }
        return shortestPath
    }
}

class Node(private val links: LinkedList<UnidirectionalLinkPush>, private val maxElements: Int) {
    private val queues =
        links.associateWith { LinkedList<Package>() } as MutableMap<UnidirectionalLinkPush, Queue<Package>>

    fun receive(p: Package, nextHop: Node?) {
        if (p.destination === this) {
            println("package arrived: $p")
            return
        } else if (nextHop === null) {
            println("no connection to package destination for: $p")
        } else if (p.getPosition() !== this) {
            // TODO: show this dependence via code structure
            println("package can't be added to queue, since it is not positioned at this node")
        } else if (elementsAtNode() < maxElements) {
            passToQueue(p, nextHop)
        }
    }

    fun arrivedVia(link: UnidirectionalLinkPush) {
        link.resetOccupyWith()
        val queue = queues[link]
        removeFromQueue(queue!!)
        tryTransfer(link)
    }

    fun addLink(link: UnidirectionalLinkPush) {
        this.links.add(link)
        this.queues[link] = LinkedList()
    }

    fun getLinks(): List<UnidirectionalLinkPush> {
        return this.links
    }

    private fun removeFromQueue(queue: Queue<Package>) {
        queue.remove()
    }

    private fun passToQueue(p: Package, nextHop: Node) {
        val linkToNextHop = getLinkTo(nextHop)!!
        queues[linkToNextHop]?.add(p)
        tryTransfer(linkToNextHop)
    }

    private fun tryTransfer(link: UnidirectionalLinkPush) {
        val linkQueue = queues[link]
        val nextPackage = linkQueue?.firstOrNull()
        if (link.isFree() && nextPackage !== null) {
            transferPackage(link, nextPackage)
        }
    }

    private fun transferPackage(link: UnidirectionalLinkPush, p: Package) {
        link.occupyWith(p)
        val transmissionTime = 10
        Simulator.addCallback(
            PackageArriveCallback(
                Simulator.getCurrentTimestamp() + transmissionTime,
                PackageArriveCallbackParams(p, link)
            )
        )
    }

    private fun getLinkTo(node: Node): UnidirectionalLinkPush? {
        return links.find { it.getDestination() === node }
    }


    private fun elementsAtNode(): Int {
        return this.queues.values.sumOf { it.size }
    }
}