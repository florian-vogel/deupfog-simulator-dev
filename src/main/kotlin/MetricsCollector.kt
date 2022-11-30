import java.util.*

class MetricsCollector(
    val name: String, edges: List<Edge>, servers: List<Server>, updates: List<Simulator.InitialUpdateParams>
) {
    val updateMetricsCollector = UpdateMetricsCollector(edges, servers, updates)
    // package metrics collector -> focus on lost packages (eg. queue full or edge offline)
    // simulation performace metrics
    // val requestMetricsCollector
    // TODO: val linkMetricsCollector

    fun printMetrics() {
        this.updateMetricsCollector.print()
    }
}

class UpdateMetricsCollector(
    private val edges: List<Edge>,
    private val servers: List<Server>,
    initialUpdateParams: List<Simulator.InitialUpdateParams>
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

    private val updateMetrics = initialUpdateParams.associate { it.update to UpdateMetricsOutput(it.atInstant) };

    fun onArrive(update: SoftwareUpdate, node: Node) {
        if (node is Edge) {
            onArriveAtEdge(update, node)
        } else if (node is Server) {
            onArriveAtServer(update, node)
        }
    }

    // TODO: one method that is called by the Node superclass, then work with type cases
    private fun onArriveAtEdge(update: SoftwareUpdate, edge: Edge) {
        if (updateArrivedAtAllEdges(update)) {
            updateMetrics[update]?.arrivedAtEdgeTimeline?.add(Pair(Simulator.getCurrentTimestamp(), edge))
        } else {
            updateMetrics[update]?.arrivedAtEdgeTimeline?.add(Pair(Simulator.getCurrentTimestamp(), edge))
            if (updateArrivedAtAllEdges(update)) {
                updateMetrics[update]?.arrivedAtAllEdgesAt = Simulator.getCurrentTimestamp()
            }
        }
    }

    private fun onArriveAtServer(update: SoftwareUpdate, server: Server) {
        if (updateArrivedAtAllServers(update)) {
            updateMetrics[update]?.arrivedAtServerTimeline?.add(Pair(Simulator.getCurrentTimestamp(), server))
        } else {
            updateMetrics[update]?.arrivedAtServerTimeline?.add(Pair(Simulator.getCurrentTimestamp(), server))
            if (updateArrivedAtAllServers(update)) {
                updateMetrics[update]?.arrivedAtAllServersAt = Simulator.getCurrentTimestamp()
            }
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
            println("    arrivedAtServerTimeline: ${it.value.arrivedAtServerTimeline}")
            println("    arrivedAtEdgeTimeline: ${it.value.arrivedAtEdgeTimeline}")
        }
    }
}
