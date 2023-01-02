package metrics

import simulator.Simulator
import java.util.*

class ResourcesUsageMetricsCollector() : Metrics {
    private val bandwidthUsageTimeline: PriorityQueue<TimestampToInt> = PriorityQueue { c1, c2 ->
        c1.timestamp.compareTo(c2.timestamp)
    }
    private var successfulDataSendInTotal: Int = 0

    fun onLinkOccupied(linkBandwidth: Int) {
        val latestBandwidth = bandwidthUsageTimeline.lastOrNull()?.value ?: 0
        val newBandwidth = latestBandwidth + linkBandwidth
        val latestValueAtCurrentTimestamp =
            bandwidthUsageTimeline.lastOrNull()?.timestamp == Simulator.getCurrentTimestamp()
        if (latestValueAtCurrentTimestamp) {
            bandwidthUsageTimeline.lastOrNull()?.value = newBandwidth
        } else {
            bandwidthUsageTimeline.add(
                TimestampToInt(Simulator.getCurrentTimestamp(), newBandwidth)
            )
        }
    }

    fun onLinkFreedUp(linkBandwidth: Int) {
        val latestBandwidth = bandwidthUsageTimeline.lastOrNull()?.value ?: 0
        val newBandwidth = latestBandwidth - linkBandwidth
        val latestValueAtCurrentTimestamp =
            bandwidthUsageTimeline.lastOrNull()?.timestamp == Simulator.getCurrentTimestamp()
        if (latestValueAtCurrentTimestamp) {
            bandwidthUsageTimeline.lastOrNull()?.value = newBandwidth
        } else {
            bandwidthUsageTimeline.add(
                TimestampToInt(Simulator.getCurrentTimestamp(), newBandwidth)
            )
        }
    }

    fun packageArrivedSuccessfully(size: Int) {
        successfulDataSendInTotal += size
    }

    override fun writeToCsv(path: String) {
        writeBandwidthUsageTimelineToCsv(path)
    }

    override fun printSummaryToConsole() {
        println(
            "resources usage \n" +
                    "successful-data-send in total: $successfulDataSendInTotal \n"
        )
    }

    private fun writeBandwidthUsageTimelineToCsv(resourceUsageMetricsPath: String) {
        val path = "$resourceUsageMetricsPath/bandwidthUsageTimeline.csv"
        val timestampToBandwidthUsage = mutableListOf<TimestampToInt>()
        bandwidthUsageTimeline.groupBy { v -> v.timestamp }.forEach { (key, value) ->
            val latest = timestampToBandwidthUsage.lastOrNull()
            // todo
            // groupby mit count hier falsch
            timestampToBandwidthUsage.add(TimestampToInt(key, latest?.value?.plus(value.count()) ?: value.count()))
        }
        writeCsv(timestampToBandwidthUsage, path)
    }
}
