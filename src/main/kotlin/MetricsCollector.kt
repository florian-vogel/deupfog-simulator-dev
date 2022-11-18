import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

class MetricsCollector<TUpdatable : UpdatableType>(
    val name: String, edges: List<Edge<TUpdatable>>, servers: List<Server<TUpdatable>>
) {
    // val linkMetricsCollector = LinkMetricsCollector()
    val updateMetricsCollector = UpdateMetricsCollector(edges, servers)
    // private val edgeRegistry = mutableMapOf<Software, LinkedList<EdgeNode>>()
    // private val serverRegistry: LinkedList<ServerNode> = LinkedList()

    fun printMetrics() {
        // this.linkMetricsCollector.printMetrics()
        this.updateMetricsCollector.print()
    }
}

class UpdateMetricsCollector(
    private val edges: List<Edge<out UpdatableType>>, private val servers: List<Server<out UpdatableType>>
) {
    data class UpdateMetricsOutput(
        val initializedAt: Int = Simulator.getCurrentTimestamp(),
        val arrivedAtServerTimeline: MutableMap<Int, Server<out UpdatableType>> = mutableMapOf(),
        var arrivedAtAllServersAt: Int? = null,
        val arrivedAtEdgeTimeline: PriorityQueue<Pair<Int, Edge<out UpdatableType>>> = PriorityQueue { c1, c2 ->
            c1.first.compareTo(c2.first)
        },
        var arrivedAtAllEdgesAt: Int? = null,
    )

    val updates = mutableMapOf<UpdatableUpdate<out UpdatableType>, UpdateMetricsOutput>();


    fun registerUpdate(update: UpdatableUpdate<out UpdatableType>) {
        updates[update] = UpdateMetricsOutput()
    }

    fun onArriveAtEdge(update: UpdatableUpdate<out UpdatableType>, edge: Edge<out UpdatableType>) {
        updates[update]?.arrivedAtEdgeTimeline?.add(Pair(Simulator.getCurrentTimestamp(), edge))
        if (updateArrivedAtAllEdges(update)) {
            updates[update]?.arrivedAtAllEdgesAt = Simulator.getCurrentTimestamp()
        }
    }

    private fun updateArrivedAtAllEdges(update: UpdatableUpdate<out UpdatableType>): Boolean {
        return edges.all { updates[update]?.arrivedAtEdgeTimeline?.map { pair -> pair.second }?.contains(it) == true }
    }

    fun print() {
        updates.forEach {
            println("update: ${it.key} | initializedAt: ${it.value.initializedAt} | arrivedAtAllServers: ${it.value.arrivedAtAllServersAt} | arrivedAtAllEdges: ${it.value.arrivedAtAllEdgesAt}")
        }
    }
}
