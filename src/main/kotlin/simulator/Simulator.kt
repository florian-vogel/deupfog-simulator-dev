package simulator

import network.UpdatePackage
import metrics.MetricsCollector
import network.Network
import node.Server
import software.SoftwareUpdate
import java.util.PriorityQueue

data class InitialUpdateParams(
    val update: SoftwareUpdate, val atInstant: Int, val initiallyAvailableAt: List<Server>
)

data class SimulatorConfig(
    val maxSimDuration: Int?,
    val printConsoleMetrics: Boolean = true,
    val writeCsvMetrics: Boolean = true,
)

data class SimulationSetup(
    val network: Network,
    val updatesParams: List<InitialUpdateParams>
)

val defaultSimulatorConfig = SimulatorConfig(
    100000,
)

const val csvOutDirPath = "./metrics/csv_data"

class Simulator(
    private val setup: SimulationSetup,
    private var config: SimulatorConfig = defaultSimulatorConfig
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

        if (config.printConsoleMetrics) {
            metrics?.printSummaryToConsole()
        }
        if (config.writeCsvMetrics) {
            metrics?.writeMetricsToCsv(csvOutDirPath)
        }
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
