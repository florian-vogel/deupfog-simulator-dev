import java.io.File
import java.util.*

interface Metrics {
    fun writeToCsv()
}

class MetricsCollector(
    val name: String,
    edges: List<Edge>,
    servers: List<Server>,
    links: List<UnidirectionalLink>,
    updates: List<Simulator.InitialUpdateParams>
) {
    val updateMetricsCollector = UpdateMetricsCollector(edges, servers, updates)
    val linkMetricsCollector = LinkMetricsCollector(links)
    val nodeMetricsCollector = NodeMetricsCollector(edges + servers)
    // package metrics collector -> focus on lost packages (eg. queue full or edge offline)
    // simulation performace metrics
    // val requestMetricsCollector
    // TODO: val linkMetricsCollector

    fun printAndGetGraph() {
        this.updateMetricsCollector.print()
        val asCsv = this.updateMetricsCollector.writeToCsv()
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

    class TimestampToCount(private val timestamp: Int, private val count: Int) : CsvWritable {

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
            var counter = 0
            val toObj = arrivedAtServerTimeline.map { pair -> counter++; TimestampToCount(pair.first, counter) }
            writeCsv(toObj, path, true)
        }
    }

    private fun writeArrivedAtEdgeTimelineToCSV() {
        updateMetrics.forEach() {
            // TODO: arrayList kann wahrscheinlich weg
            val arrivedAtEdgeTimeline = ArrayList(it.value.arrivedAtEdgeTimeline)
            val simulationName = Simulator.simulationName
            val path = "./analysis/stats-out/${simulationName}/updateMetrics/${it.key}/arrivedAtEdgeTimeline.csv"
            var counter = 0
            val toObj = arrivedAtEdgeTimeline.map { pair -> counter++; TimestampToCount(pair.first, counter) }
            writeCsv(toObj, path, true)
        }
    }

    // TODO: weitere ergänzen
}

class LinkMetricsCollector(val links: List<UnidirectionalLink>) : Metrics {
    enum class LinkState {
        FREE, OCCUPIED, OFFLINE
    }

    data class LinkMetricsOutput(
        val linkStateTimeline: PriorityQueue<Pair<Int, LinkState>> = PriorityQueue { c1, c2 ->
            c1.first.compareTo(c2.first)
        }
    )

    private val linkMetrics = links.associateWith { LinkMetricsOutput() };


    class LinkStateMonitor(
        private val timestamp: Int,
        private val linksFree: Int,
        private val linksOccupied: Int,
        private val linksOffline: Int
    ) : CsvWritable {

        override fun toCsv(): List<CsvWritableObject> {
            return listOf(
                CsvWritableObject("timestamp", timestamp.toString()),
                CsvWritableObject("linksFree", linksFree.toString()),
                CsvWritableObject("linksOccupied", linksOccupied.toString()),
                CsvWritableObject("linksOffline", linksOffline.toString()),
            )
        }
    }

    private val linkStateMonitorTimeline = mutableListOf(
        LinkStateMonitor(0, links.count {
            it.isOnline() && it.hasUnusedBandwidth()
        }, links.count {
            it.isOnline() && !it.hasUnusedBandwidth()
        }, links.count {
            !it.isOnline()
        })
    )


    fun onChangedLinkState(link: UnidirectionalLink) {
        val lastLinkState = linkMetrics[link]?.linkStateTimeline?.peek()?.second
        var newLinkState: LinkState?
        if (link.isOnline() && link.hasUnusedBandwidth()) {
            newLinkState = LinkState.FREE
        } else if (link.isOnline() && !link.hasUnusedBandwidth()) {
            newLinkState = LinkState.OCCUPIED
        } else {
            newLinkState = LinkState.OFFLINE
        }
        if (lastLinkState === null || lastLinkState !== newLinkState) {
            linkMetrics[link]?.linkStateTimeline?.add(Pair(Simulator.getCurrentTimestamp(), newLinkState))

            val newLinkStateMonitor = LinkStateMonitor(0, links.count {
                it.isOnline() && it.hasUnusedBandwidth()
            }, links.count {
                it.isOnline() && !it.hasUnusedBandwidth()
            }, links.count {
                !it.isOnline()
            })

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
        val path = "./analysis/stats-out/${Simulator.simulationName}/linkStateMonitor/linkStateMonitorTimeline.csv"
        writeCsv(linkStateMonitorTimeline, path, true)
    }
}

class NodeMetricsCollector(nodes: List<Node>) : Metrics {

    data class NodeMetricsOutput(
        val packagesInQueueTimeline: PriorityQueue<PackageLostTimestamp> = PriorityQueue { c1, c2 ->
            c1.timestamp.compareTo(c2.timestamp)
        }
    )

    private val nodeMetrics = nodes.associateWith { NodeMetricsOutput() };


    fun onPackageLost(node: Node) {
        if (nodeMetrics[node] !== null) {
            nodeMetrics[node]!!.packagesInQueueTimeline.add(PackageLostTimestamp(Simulator.getCurrentTimestamp()))
        }
    }

    override fun writeToCsv() {
        writePackageLostTimelineToCsv()
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
            writeCsv(it.value.packagesInQueueTimeline.toList(), path, true)
        }
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
