package simulator

import metrics.MetricsCollector
import network.Network
import network.UpdatePackage
import node.Server
import software.SoftwareUpdate
import java.util.*


data class InitialUpdateParams(
    val update: SoftwareUpdate, val atInstant: Int, val initiallyAvailableAt: List<Server>
)

data class SimulatorConfig(
    val name: String,
    val maxSimDuration: Int?,
    val printConsoleMetrics: Boolean = true,
    val writeCsvMetrics: Boolean = true,
)

data class SimulationSetup(
    val network: Network,
    val updatesParams: List<InitialUpdateParams>
)

const val csvOutDirPath = "./metrics/csv_data/"

class Simulator(
    val setup: SimulationSetup,
    private var config: SimulatorConfig
) {
    companion object {
        private var currentTimestamp: Int = 0
        private val callbacks: PriorityQueue<TimedCallback> = PriorityQueue { c1, c2 ->
            c1.atInstant.compareTo(c2.atInstant)
        }
        private var metrics: MetricsCollector? = null

        fun addCallback(c: TimedCallback) {
            callbacks.add(c)
        }

        fun cancelCallback(c: TimedCallback) {
            callbacks.remove(c)
        }

        fun getCurrentTimestamp(): Int {
            return currentTimestamp
        }

        fun setTimestamp(value: Int) {
            currentTimestamp = value
        }

        fun getMetrics(): MetricsCollector? {
            return metrics
        }

        fun reset(){
            currentTimestamp = 0
            callbacks.clear()
            metrics = null

        }
    }

    private fun setMetrics(metricsCollector: MetricsCollector) {
        metrics = metricsCollector
    }

    fun runSimulation() {
        setMetrics(
            MetricsCollector(
                setup.network.edges,
                setup.network.servers,
                setup.updatesParams
            )
        )
        setup.network.initNetwork()
        processInitialUpdates(setup.updatesParams)

        while (true) {
            if (callbacks.isEmpty() || getCurrentTimestamp() > config.maxSimDuration!!) {
                break
            }
            val currentCallback = callbacks.poll()

            setTimestamp(currentCallback.atInstant)
            currentCallback.runCallback()
        }

        if (config.writeCsvMetrics) {
            metrics?.writeMetricsToCsv(csvOutDirPath + config.name + '/')
        }
        if (config.printConsoleMetrics) {
            metrics?.printSummaryToConsoleAndToFile(csvOutDirPath + config.name + '/' + "console_log.txt")
        }

        reset()
    }

    private fun processInitialUpdates(updates: List<InitialUpdateParams>) {
        updates.stream().forEach {
            for (updateInitNode in it.initiallyAvailableAt) {
                val p = UpdatePackage(
                    updateInitNode,
                    updateInitNode,
                    setup.network.networkConfig.defaultPackageConfig.updatePackageOverhead,
                    it.update
                )
                val initUpdateCallback = TimedCallback(it.atInstant) {
                    updateInitNode.initializeUpdate(p)
                }
                callbacks.add(initUpdateCallback)
            }
        }
    }
}
