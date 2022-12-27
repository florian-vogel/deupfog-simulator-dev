import main.Simulator
import node.Edge
import node.Node
import node.Server
import software.SoftwareUpdate
import java.io.File
import java.util.*

interface Metrics {
    fun writeToCsv()
}

class MetricsCollector(
    val name: String, edges: List<Edge>, servers: List<Server>, updates: List<Simulator.InitialUpdateParams>
) {
    private val links: List<UnidirectionalLink> = (edges + servers).flatMap { it.getLinks().orEmpty() }
    val updateMetricsCollector = UpdateMetricsCollector(edges, servers, updates)
    val linkMetricsCollector = LinkMetricsCollector(links)
    val nodeMetricsCollector = NodeMetricsCollector(edges + servers)
    // package metrics collector -> focus on lost packages (eg. queue full or edge offline)
    // simulation performace metrics
    // val requestMetricsCollector
    // TODO: val linkMetricsCollector

    fun printAndGetGraph() {
        this.updateMetricsCollector.print()
        this.updateMetricsCollector.writeToCsv()
        this.linkMetricsCollector.writeToCsv()
        this.nodeMetricsCollector.writeToCsv()
    }
}

class UpdateMetricsCollector(
    // edge should be instead UpdateReceiverNode
    private val edges: List<Edge>,
    private val servers: List<Server>,
    initialUpdateParams: List<Simulator.InitialUpdateParams>
) : Metrics {
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
            // updateMetrics[update]?.arrivedAtEdgeTimeline?.add(Pair(Simulator.getCurrentTimestamp(), edge))
        } else {
            updateMetrics[update]?.arrivedAtEdgeTimeline?.add(Pair(Simulator.getCurrentTimestamp(), edge))
            if (updateArrivedAtAllEdges(update)) {
                updateMetrics[update]?.arrivedAtAllEdgesAt = Simulator.getCurrentTimestamp()
            }
        }
    }

    private fun onArriveAtServer(update: SoftwareUpdate, server: Server) {
        if (updateArrivedAtAllServers(update)) {
            // updateMetrics[update]?.arrivedAtServerTimeline?.add(Pair(Simulator.getCurrentTimestamp(), server))
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

    class TimestampToCount(private val timestamp: Int, val count: Int) : CsvWritable {

        override fun toCsv(): List<CsvWritableObject> {
            return listOf(
                CsvWritableObject("timestamp", timestamp.toString()), CsvWritableObject("count", count.toString())
            )
        }
    }

    override fun writeToCsv() {
        writeArrivedAtServerTimelineToCSV()
        writeArrivedAtEdgeTimelineToCSV()
    }

    private fun writeArrivedAtServerTimelineToCSV() {
        updateMetrics.forEach() {
            val arrivedAtServerTimeline = ArrayList(it.value.arrivedAtServerTimeline)
            val simulationName = Simulator.simulationName
            val path = "./analysis/stats-out/${simulationName}/updateMetrics/${it.key}/arrivedAtServerTimeline.csv"
            val arrivesAtTimestamp = mutableListOf<TimestampToCount>()
            arrivedAtServerTimeline.groupBy { v -> v.first }.forEach { (key, value) ->
                val latest = arrivesAtTimestamp.lastOrNull()
                arrivesAtTimestamp.add(TimestampToCount(key, latest?.count?.plus(value.count()) ?: value.count()))
            }
            writeCsv(arrivesAtTimestamp, path, true)
        }
    }

    private fun writeArrivedAtEdgeTimelineToCSV() {
        updateMetrics.forEach() {
            // TODO: arrayList kann wahrscheinlich weg
            val arrivedAtEdgeTimeline = ArrayList(it.value.arrivedAtEdgeTimeline)
            val simulationName = Simulator.simulationName
            val path = "./analysis/stats-out/${simulationName}/updateMetrics/${it.key}/arrivedAtEdgeTimeline.csv"
            val arrivesAtTimestamp = mutableListOf<TimestampToCount>()
            arrivedAtEdgeTimeline.groupBy { v -> v.first }.forEach { (key, value) ->
                val latest = arrivesAtTimestamp.lastOrNull()
                arrivesAtTimestamp.add(TimestampToCount(key, latest?.count?.plus(value.count()) ?: value.count()))
            }
            writeCsv(arrivesAtTimestamp, path, true)
        }
    }

    // TODO: weitere ergänzen
}

class LinkMetricsCollector(private val links: List<UnidirectionalLink>) : Metrics {
    enum class LinkState {
        FREE, OCCUPIED, OFFLINE
    }

    data class LinkData(
        val linkStateTimeline: PriorityQueue<Pair<Int, LinkState>> = PriorityQueue { c1, c2 ->
            c1.first.compareTo(c2.first)
        }
    )

    private val linkData = links.associateWith { LinkData() };


    class LinkStateMonitor(
        val timestamp: Int, val linksFree: Int, val linksOccupied: Int, val linksOffline: Int
    ) : CsvWritable {

        override fun toCsv(): List<CsvWritableObject> {
            return listOf(
                CsvWritableObject("timestamp", timestamp.toString()),
                //CsvWritableObject("linksFree", linksFree.toString()),
                CsvWritableObject("linksOccupied", linksOccupied.toString()),
                CsvWritableObject("linksOffline", linksOffline.toString()),
            )
        }
    }

    private val linkStateMonitorTimeline = mutableListOf(
        LinkStateMonitor(0, links.count {
            it.getOnlineState() && it.hasUnusedBandwidth()
        }, links.count {
            it.getOnlineState() && !it.hasUnusedBandwidth()
        }, links.count {
            !it.getOnlineState()
        })
    )


    fun onChangedLinkState(link: UnidirectionalLink) {
        val lastLinkState = linkData[link]?.linkStateTimeline?.peek()?.second
        val newLinkState: LinkState = if (link.getOnlineState() && link.hasUnusedBandwidth()) {
            LinkState.FREE
        } else if (link.getOnlineState() && !link.hasUnusedBandwidth()) {
            LinkState.OCCUPIED
        } else {
            LinkState.OFFLINE
        }
        if (lastLinkState === null || lastLinkState !== newLinkState) {
            linkData[link]?.linkStateTimeline?.add(Pair(Simulator.getCurrentTimestamp(), newLinkState))

            val newLinkStateMonitor = LinkStateMonitor(Simulator.getCurrentTimestamp(), links.count {
                it.getOnlineState() && it.hasUnusedBandwidth()
            }, links.count {
                it.getOnlineState() && !it.hasUnusedBandwidth()
            }, links.count {
                !it.getOnlineState()
            })

            linkStateMonitorTimeline.removeIf { it.timestamp == newLinkStateMonitor.timestamp }

            val lastElement = linkStateMonitorTimeline.lastOrNull()
            if (lastElement != null) {
                val copyLast = LinkStateMonitor(
                    Simulator.getCurrentTimestamp() - 1,
                    lastElement.linksFree,
                    lastElement.linksOccupied,
                    lastElement.linksOffline
                )
                linkStateMonitorTimeline.add(copyLast)
            }

            // TODO: optimise efficiency
            linkStateMonitorTimeline.add(
                newLinkStateMonitor
            )
        }

    }

    override fun writeToCsv() {
        writeLinkStateMonitorTimelineToCsv()
    }

    private fun writeLinkStateMonitorTimelineToCsv() {
        val path = "./analysis/stats-out/${Simulator.simulationName}/linkMetrics/linkStateMonitorTimeline.csv"
        writeCsv(linkStateMonitorTimeline, path, true)
    }
}

class NodeMetricsCollector(val nodes: List<Node>) : Metrics {

    data class NodeCollectedData(
        val packageLostTimestamps: PriorityQueue<PackageLostTimestamp> = PriorityQueue { c1, c2 ->
            c1.timestamp.compareTo(c2.timestamp)
        }, val nodeStateTimeline: PriorityQueue<Pair<Int, NodeState>> = PriorityQueue { c1, c2 ->
            c1.first.compareTo(c2.first)
        }
    )

    enum class NodeState {
        ONLINE, OFFLINE
    }

    class NodeStateMonitor(
        val timestamp: Int,
        private val nodesOnline: Int,
        private val nodesOffline: Int,
    ) : CsvWritable {

        override fun toCsv(): List<CsvWritableObject> {
            return listOf(
                CsvWritableObject("timestamp", timestamp.toString()),
                CsvWritableObject("nodesOnline", nodesOnline.toString()),
                CsvWritableObject("nodesOffline", nodesOffline.toString()),
            )
        }
    }

    private val nodeStateMonitorTimeline = mutableListOf(
        NodeStateMonitor(0, nodes.count {
            it.getOnlineState()
        }, nodes.count {
            !it.getOnlineState()
        })
    )

    private val nodeMetrics = nodes.associateWith { NodeCollectedData() };


    fun onPackageLost(node: Node) {
        if (nodeMetrics[node] !== null) {
            nodeMetrics[node]!!.packageLostTimestamps.add(PackageLostTimestamp(Simulator.getCurrentTimestamp()))
        }
    }

    fun onNodeStateChanged(node: Node) {
        val lastNodeState = nodeMetrics[node]?.nodeStateTimeline?.peek()?.second
        val newLinkState: NodeState = if (node.getOnlineState()) {
            NodeState.ONLINE
        } else {
            NodeState.OFFLINE
        }
        if (lastNodeState === null || lastNodeState !== newLinkState) {
            nodeMetrics[node]?.nodeStateTimeline?.add(Pair(Simulator.getCurrentTimestamp(), newLinkState))

            val newNodeStateMonitor = NodeStateMonitor(Simulator.getCurrentTimestamp(), nodes.count {
                it.getOnlineState()
            }, nodes.count {
                !it.getOnlineState()
            })

            if (nodeStateMonitorTimeline.last().timestamp == newNodeStateMonitor.timestamp) {
                nodeStateMonitorTimeline.removeLastOrNull()
            }

            // TODO: optimise efficiency
            nodeStateMonitorTimeline.add(
                newNodeStateMonitor
            )
        }

    }

    override fun writeToCsv() {
        writePackageLostTimelineToCsv()
        writeNodeStateMonitorTimelineToCsv()
    }

    class PackageLostTimestamp(val timestamp: Int) : CsvWritable {
        override fun toCsv(): List<CsvWritableObject> {
            return listOf(
                CsvWritableObject("timestamp", timestamp.toString())
            )
        }
    }

    private fun writePackageLostTimelineToCsv() {
        nodeMetrics.forEach() {
            val path = "./analysis/stats-out/${Simulator.simulationName}/nodeMetrics/${it.key}/packageLostTimeline.csv"
            writeCsv(it.value.packageLostTimestamps.toList(), path, true)
        }
    }

    private fun writeNodeStateMonitorTimelineToCsv() {
        val path = "./analysis/stats-out/${Simulator.simulationName}/nodeMetrics/nodeStateMonitorTimeline.csv"
        writeCsv(nodeStateMonitorTimeline, path, true)
    }
}

data class CsvWritableObject(
    val columnName: String, val valueAsString: String
)

interface CsvWritable {
    fun toCsv(): List<CsvWritableObject>
}

fun <T : CsvWritable> writeCsv(data: List<T>, filePath: String, deleteIfExists: Boolean = false) {
    val file = File(filePath)
    if (deleteIfExists) {
        // TODO
        file.parentFile.delete()
    }
    file.parentFile.mkdirs()

    val out = file.outputStream().bufferedWriter()

    if (data.isEmpty()) {
        return
    }
    val obj1 = data[0]
    val header = obj1.toCsv().joinToString(",") { it.columnName }

    out.write(header)
    out.write("\n")


    data.forEach { obj ->
        val objAsCsv = obj.toCsv()
        val accumulatedString = objAsCsv.joinToString(",") { it.valueAsString }
        out.write(accumulatedString)
        out.write("\n")
    }

    out.close()
}
