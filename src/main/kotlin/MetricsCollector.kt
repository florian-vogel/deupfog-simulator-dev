import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

class MetricsCollector(
    val name: String, edges: List<Edge>, servers: List<Server>, updates: List<SoftwareUpdate>
) {
    // val linkMetricsCollector = LinkMetricsCollector()
    val updateMetricsCollector = UpdateMetricsCollector(edges, servers, updates)
    // private val edgeRegistry = mutableMapOf<Software, LinkedList<EdgeNode>>()
    // private val serverRegistry: LinkedList<ServerNode> = LinkedList()

    fun printMetrics() {
        // this.linkMetricsCollector.printMetrics()
        this.updateMetricsCollector.print()
    }
}

class UpdateMetricsCollector(
    private val edges: List<Edge>, private val servers: List<Server>, private val updates: List<SoftwareUpdate>
) {
    data class UpdateMetricsOutput(
        val initializedAt: Int,
    ) {
        val arrivedAtServerTimeline: PriorityQueue<Pair<Int, Server>> = PriorityQueue { c1, c2 ->
            c1.first.compareTo(c2.first)
        }
        var arrivedAtAllServersAt: Int? = null
        val arrivedAtEdgeTimeline: PriorityQueue<Pair<Int, Edge>> = PriorityQueue { c1, c2 ->
            c1.first.compareTo(c2.first)
        }
        var arrivedAtAllEdgesAt: Int? = null
    }

    private val updateMetrics = updates.map { it to UpdateMetricsOutput(it.initializeTimestamp) }.toMap();

    // TODO: one method that is called by the Node superclass, then work with type cases
    fun onArriveAtEdge(update: SoftwareUpdate, edge: Edge) {
        updateMetrics[update]?.arrivedAtEdgeTimeline?.add(Pair(Simulator.getCurrentTimestamp(), edge))
        if (updateArrivedAtAllEdges(update)) {
            updateMetrics[update]?.arrivedAtAllEdgesAt = Simulator.getCurrentTimestamp()
        }
    }

    fun onArriveAtServer(update: SoftwareUpdate, server: Server) {
        updateMetrics[update]?.arrivedAtServerTimeline?.add(Pair(Simulator.getCurrentTimestamp(), server))
        if (updateArrivedAtAllServers(update)) {
            updateMetrics[update]?.arrivedAtAllServersAt= Simulator.getCurrentTimestamp()
        }
    }

    private fun updateArrivedAtAllEdges(update: SoftwareUpdate): Boolean {
        return edges.all {
            updateMetrics[update]?.arrivedAtEdgeTimeline?.map { pair -> pair.second }
                ?.contains(it) == true || !it.listeningFor().map { state -> state.type }.contains(update.type)
        }
    }

    private fun updateArrivedAtAllServers(update: SoftwareUpdate): Boolean {
        return servers.all {
            updateMetrics[update]?.arrivedAtServerTimeline?.map { pair -> pair.second }
                ?.contains(it) == true || !it.listeningFor().map { state -> state.type }.contains(update.type)
        }
    }

    fun print() {
        updateMetrics.forEach {
            println("update: ${it.key} | initializedAt: ${it.value.initializedAt} | arrivedAtAllServers: ${it.value.arrivedAtAllServersAt} | arrivedAtAllEdges: ${it.value.arrivedAtAllEdgesAt}")
        }
    }
}
