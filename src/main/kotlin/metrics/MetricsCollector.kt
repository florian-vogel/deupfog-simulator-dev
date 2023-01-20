package metrics

import node.Edge
import node.Server
import simulator.InitialUpdateParams

const val updateMetricsPath = "/updateMetrics"
const val resourcesUsageMetricsPath = "/resourcesUsageMetrics"
const val packageLossMetricsPath = "/packageLossMetrics"

interface Metrics {
    fun writeToCsv(path: String)
    fun printSummaryToConsoleAndWriteToFile(path: String)
}

class MetricsCollector(
    edges: List<Edge>, servers: List<Server>, updates: List<InitialUpdateParams>
) {

    val updateMetricsCollector = UpdateMetricsCollector(edges, servers, updates)
    val resourcesUsageMetricsCollector = ResourcesUsageMetricsCollector()
    val packageLossMetricsCollector = PackageLossMetricsCollector()

    fun printSummaryToConsoleAndToFile(path: String) {
        this.updateMetricsCollector.printSummaryToConsoleAndWriteToFile(path)
        this.resourcesUsageMetricsCollector.printSummaryToConsoleAndWriteToFile(path)
        this.packageLossMetricsCollector.printSummaryToConsoleAndWriteToFile(path)
    }

    fun writeMetricsToCsv(metricsPath: String) {
        this.updateMetricsCollector.writeToCsv(metricsPath + updateMetricsPath)
        this.resourcesUsageMetricsCollector.writeToCsv(metricsPath + resourcesUsageMetricsPath)
        this.packageLossMetricsCollector.writeToCsv(metricsPath + packageLossMetricsPath)
    }
}
