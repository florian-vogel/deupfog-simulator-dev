package metrics

import simulator.Simulator
import java.util.*

class ResourcesUsageMetricsCollector() : Metrics {
    private val bandwidthUsageTimeline: PriorityQueue<TimestampToInt> = PriorityQueue { c1, c2 ->
        c1.timestamp.compareTo(c2.timestamp)
    }
    private var successfulDataSendInTotal: Int = 0
    private var processingTimeInTotal: Int = 0

    fun onLinkOccupied(linkBandwidth: Int) {
        addBandwidthUsageTimelineMetric(linkBandwidth)
    }

    fun onLinkFreedUp(linkBandwidth: Int) {
        addBandwidthUsageTimelineMetric(-linkBandwidth)
    }

    fun onProcessPackage(processingTime: Int) {
        processingTimeInTotal += processingTime
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
                    "successful-data-send in total: $successfulDataSendInTotal \n" +
                    "processing-time in total: $processingTimeInTotal \n"
        )
    }

    private fun writeBandwidthUsageTimelineToCsv(resourceUsageMetricsPath: String) {
        val path = "$resourceUsageMetricsPath/bandwidthUsageTimeline.csv"
        writeCsv(bandwidthUsageTimeline.toList(), path)
    }

    private fun addBandwidthUsageTimelineMetric(valueChange: Int) {
        val latestValue = bandwidthUsageTimeline.lastOrNull()
        if (latestValue != null && latestValue.timestamp < Simulator.getCurrentTimestamp()) {
            bandwidthUsageTimeline.add(TimestampToInt(Simulator.getCurrentTimestamp() - 1, latestValue.value))
        }

        val latestBandwidthMetric = latestValue?.value ?: 0
        val currentBandwidth = latestBandwidthMetric + valueChange
        val duplicateValueAtTimestamp =
            latestValue?.timestamp == Simulator.getCurrentTimestamp()
        if (duplicateValueAtTimestamp) {
            latestValue?.value = currentBandwidth
        } else {
            bandwidthUsageTimeline.add(
                TimestampToInt(Simulator.getCurrentTimestamp(), currentBandwidth)
            )
        }
    }
}
