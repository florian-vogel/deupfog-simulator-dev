import java.util.*

// todo: nochmal gucken wie andere simulatoren die werte abstrahiert haben
// class ByteValue
// class InstantValue

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
        from.getLinks()?.filter { !alreadyVisited.contains<Node>(it.to) && it.to.getOnlineState() }

    if (linksAtFromNodeToUnvisited.isNullOrEmpty()) {
        return null
    } else {
        var shortestPath: LinkedList<Node>? = null
        linksAtFromNodeToUnvisited.forEach {
            val subPath = findShortestPathRek(it.to, to, HashSet(alreadyVisited.plus(from)))
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