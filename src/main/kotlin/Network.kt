import java.util.*

class Network {
    fun findShortestPath(from: Node, to: Node): LinkedList<Node>? {
        val shortestPath = findShortestPathRek(from, to, HashSet())
        shortestPath?.poll()
        return shortestPath
    }

    // TODO: not efficient (not dijkstra) and no link-specific-consts (delay, bandwidth, queue-waiting-time)
    private fun findShortestPathRek(from: Node, to: Node, alreadyVisited: Set<Node>): LinkedList<Node>? {
        if (from === to) {
            val list = LinkedList<Node>()
            list.add(from)
            return list
        }
        val linksAtFromNodeToUnvisited =
            from.getLinks().filter { !alreadyVisited.contains<Node>(it.getDestination()) }

        if (linksAtFromNodeToUnvisited.isEmpty()) {
            return null
        } else {
            var shortestPath: LinkedList<Node>? = null
            linksAtFromNodeToUnvisited.forEach {
                val subPath = findShortestPathRek(it.getDestination(), to, HashSet(alreadyVisited.plus(from)))
                if (subPath !== null) {

                    val path = LinkedList<Node>()
                    path.add(from)
                    path.addAll(subPath)
                    if (path.last() === to && (shortestPath === null || path.size < shortestPath!!.size)) {
                        shortestPath = path
                    }
                }
            }
            return shortestPath
        }
    }
}

class Node(private val links: LinkedList<UnidirectionalLink>, private val maxElements: Int) {
    private val queues =
        links.associateWith { LinkedList<Package>() } as MutableMap<UnidirectionalLink, Queue<Package>>

    fun receive(p: Package, nextHop: Node?) {
        if (p.getDestination() === this) {
            println("package arrived: $p, ID: ${p.ID}")
            return
        } else if (nextHop === null) {
            println("no connection to package destination for: $p, ID: ${p.ID}")
        } else if (p.getPosition() !== this) {
            // TODO: show this dependence via code structure
            println("package can't be added to queue, since it is not positioned at this node")
        } else if (elementsAtNode() < maxElements) {
            passToQueue(p, nextHop)
        }
    }

    fun arrivedVia(link: UnidirectionalLink) {
        link.removeFirst()
    }

    fun addLink(link: UnidirectionalLink) {
        this.links.add(link)
        this.queues[link] = LinkedList()
    }

    fun getLinks(): List<UnidirectionalLink> {
        return this.links
    }

    private fun removeFromQueue(queue: Queue<Package>) {
        queue.poll()
    }

    private fun passToQueue(p: Package, nextHop: Node) {
        getLinkTo(nextHop)!!.lineUpPackage(p)
    }


    fun getLinkTo(node: Node): UnidirectionalLink? {
        return links.find { it.getDestination() === node }
    }


    private fun elementsAtNode(): Int {
        return this.queues.values.sumOf { it.size }
    }
}