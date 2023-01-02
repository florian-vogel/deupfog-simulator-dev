package metrics

import node.Edge
import node.Server
import simulator.InitialUpdateParams

const val updateMetricsPath = "/updateMetrics"
const val resourcesUsageMetricsPath = "/resourcesUsageMetrics"
const val packageLossMetricsPath = "/packageLossMetrics"

interface Metrics {
    fun writeToCsv(path: String)
    fun printSummaryToConsole()
}

class MetricsCollector(
    edges: List<Edge>, servers: List<Server>, updates: List<InitialUpdateParams>
) {

    val updateMetricsCollector = UpdateMetricsCollector(edges, servers, updates)
    val resourcesUsageMetricsCollector = ResourcesUsageMetricsCollector()
    val packageLossMetricsCollector = PackageLossMetricsCollector()

    fun printSummaryToConsole() {
        this.updateMetricsCollector.printSummaryToConsole()
        this.resourcesUsageMetricsCollector.printSummaryToConsole()
        this.packageLossMetricsCollector.printSummaryToConsole()
    }

    fun writeMetricsToCsv(metricsPath: String) {
        this.updateMetricsCollector.writeToCsv(metricsPath + updateMetricsPath)
        this.resourcesUsageMetricsCollector.writeToCsv(metricsPath + resourcesUsageMetricsPath)
        this.packageLossMetricsCollector.writeToCsv(metricsPath + packageLossMetricsPath)
    }
}
