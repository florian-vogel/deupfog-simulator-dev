package metrics

import simulator.Simulator
import java.util.*

class PackageLossMetricsCollector() : Metrics {
    private val packageLossTimeline: PriorityQueue<TimestampToInt> = PriorityQueue { c1, c2 ->
        c1.timestamp.compareTo(c2.timestamp)
    }
    private var totalPackageLossCount: Int = 0

    fun onPackageLost() {
        packageLossTimeline.add(
            TimestampToInt(Simulator.getCurrentTimestamp(), 1)
        )
        totalPackageLossCount++;
    }

    override fun writeToCsv(path: String) {
        writePackageLossTimelineToCsv(path)
    }

    override fun printSummaryToConsole() {
        println(
            "package loss \n" +
                    "total package-loss-count: $totalPackageLossCount \n"
        )
    }

    private fun writePackageLossTimelineToCsv(resourceUsageMetricsPath: String) {
        val path = "$resourceUsageMetricsPath/packageLossTimeline.csv"
        writeCsv(packageLossTimeline.toList(), path)
    }
}

