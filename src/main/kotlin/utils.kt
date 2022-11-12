import java.util.*

fun findShortestPath(from: Node, to: Node): LinkedList<Node>? {
    val shortestPath = findShortestPathRek(from, to)
    shortestPath?.poll()
    return shortestPath
}

// TODO: not efficient (not dijkstra) and no link-specific-consts (delay, bandwidth, queue-waiting-time)
private fun findShortestPathRek(from: Node, to: Node): LinkedList<Node>? {
    if (from === to) {
        val list = LinkedList<Node>()
        list.add(from)
        return list
    }
    val linksAtFromNode = from.getLinks()
    if (linksAtFromNode.isEmpty()) {
        return null
    } else {
        var shortestPath: LinkedList<Node>? = null
        linksAtFromNode.forEach {
            val subPath = findShortestPathRek(it.to, to)
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

fun arrivedAtDestination(p: Package, currentPosition: Node): Boolean {
    return p.destination == currentPosition
}