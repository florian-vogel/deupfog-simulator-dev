package metrics

import simulator.Simulator
import java.io.File
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
        totalPackageLossCount++
    }

    override fun writeToCsv(path: String) {
        writePackageLossTimelineToCsv(path)
    }

    override fun printSummaryToConsoleAndWriteToFile(path: String) {
        val text =
            "package loss \n" +
                    "total package-loss-count: $totalPackageLossCount \n" +
                    "\n"

        println(text)

        val file = File(path)
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        file.appendText(text)
    }

    private fun writePackageLossTimelineToCsv(resourceUsageMetricsPath: String) {
        val path = "$resourceUsageMetricsPath/packageLossTimeline.csv"
        writeCsv(packageLossTimeline.toList(), path)
    }
}

