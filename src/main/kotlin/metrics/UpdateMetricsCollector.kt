package metrics

import node.Edge
import node.Node
import node.Server
import simulator.InitialUpdateParams
import simulator.Simulator
import software.SoftwareUpdate
import java.io.File
import java.util.*

class TimestampToInt(val timestamp: Int, var value: Int) : CsvWritable {
    override fun toCsv(): List<ColumnValue> {
        return listOf(
            ColumnValue("timestamp", timestamp.toString()), ColumnValue("count", value.toString())
        )
    }
}

class UpdateMetricsCollector(
    private val edges: List<Edge>, private val servers: List<Server>, initialUpdateParams: List<InitialUpdateParams>
) : Metrics {
    data class MetricsPerUpdate(
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

    private val metrics = initialUpdateParams.associate { it.update to MetricsPerUpdate(it.atInstant) };

    fun onArrive(update: SoftwareUpdate, node: Node) {
        if (node is Edge) {
            onArriveAtEdge(update, node)
        } else if (node is Server) {
            onArriveAtServer(update, node)
        }
    }

    override fun writeToCsv(path: String) {
        writeArrivedAtServerTimelineToCSV(path)
        writeArrivedAtEdgeTimelineToCSV(path)
    }

    override fun printSummaryToConsoleAndWriteToFile(path: String) {
        var text = ""
        metrics.forEach {
            text +=
                "update: ${it.key} \n" +
                        "initializedAt: ${it.value.initializedAt} \n" +
                        "arrivedAtAllServers: ${it.value.arrivedAtAllServersAt} \n" +
                        "arrivedAtAllEdges: ${it.value.arrivedAtAllEdgesAt} \n" +
                        "arrivedAtServerTimeline: ${it.value.arrivedAtServerTimeline} \n" +
                        "arrivedAtEdgeTimeline: ${it.value.arrivedAtEdgeTimeline} \n" +
                        "\n"
        }

        println(text)

        val file = File(path)
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }

        //val out = file.outputStream().bufferedWriter()
        //out.append(text)
        //out.close()
        file.appendText(text)
    }

    private fun onArriveAtEdge(update: SoftwareUpdate, edge: Edge) {
        metrics[update]?.arrivedAtEdgeTimeline?.add(Pair(Simulator.getCurrentTimestamp(), edge))
        if (metrics[update]?.arrivedAtAllEdgesAt == null && updateArrivedAtAllEdges(update)) {
            metrics[update]?.arrivedAtAllEdgesAt = Simulator.getCurrentTimestamp()
        }
    }

    private fun onArriveAtServer(update: SoftwareUpdate, server: Server) {
        metrics[update]?.arrivedAtServerTimeline?.add(Pair(Simulator.getCurrentTimestamp(), server))
        if (metrics[update]?.arrivedAtAllServersAt == null && updateArrivedAtAllServers(update)) {
            metrics[update]?.arrivedAtAllServersAt = Simulator.getCurrentTimestamp()
        }
    }

    private fun updateArrivedAtAllEdges(update: SoftwareUpdate): Boolean {
        return edges.all {
            metrics[update]?.arrivedAtEdgeTimeline?.map { pair -> pair.second }
                ?.contains(it) == true || !it.softwareInformation().updateNeeded(update)
        }
    }

    private fun updateArrivedAtAllServers(update: SoftwareUpdate): Boolean {
        return servers.all {
            metrics[update]?.arrivedAtServerTimeline?.map { pair -> pair.second }
                ?.contains(it) == true || !it.softwareInformation().updateNeeded(update)
        }
    }

    private fun writeArrivedAtServerTimelineToCSV(updateMetricsPath: String) {
        metrics.forEach {
            val path = "${updateMetricsPath}/${it.key}/arrivedAtServerTimeline.csv"
            val arrivedAtServerTimeline = it.value.arrivedAtServerTimeline
            val timestampToArrivedCount = mutableListOf<TimestampToInt>()
            arrivedAtServerTimeline.groupBy { v -> v.first }.forEach { (key, value) ->
                val latest = timestampToArrivedCount.lastOrNull()
                timestampToArrivedCount.add(TimestampToInt(key, latest?.value?.plus(value.count()) ?: value.count()))
            }
            writeCsv(timestampToArrivedCount, path)
        }
    }

    private fun writeArrivedAtEdgeTimelineToCSV(updateMetricsPath: String) {
        metrics.forEach() {
            val path = "${updateMetricsPath}/${it.key}/arrivedAtEdgeTimeline.csv"
            val arrivedAtEdgeTimeline = it.value.arrivedAtEdgeTimeline
            val timestampToArrivedCount = mutableListOf<TimestampToInt>()
            arrivedAtEdgeTimeline.groupBy { v -> v.first }.forEach { (key, value) ->
                val latest = timestampToArrivedCount.lastOrNull()
                timestampToArrivedCount.add(TimestampToInt(key, latest?.value?.plus(value.count()) ?: value.count()))
            }
            writeCsv(timestampToArrivedCount, path)
        }
    }
}
