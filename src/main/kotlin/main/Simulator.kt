package main

import TimedCallback
import UpdatePackage
import network.Network
import node.Server
import software.SoftwareUpdate
import java.util.PriorityQueue

class Simulator(
    private val params: SimulationParams,
    private val simConfigParams: SimConfigParams
) {
    companion object {
        private var currentTimestamp: Int = 0
        private val callbacks: PriorityQueue<TimedCallback> = PriorityQueue { c1, c2 ->
            c1.atInstant.compareTo(c2.atInstant)
        }
        var metrics: MetricsCollector? = null
        val simulationName = "Simulation01"

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

        fun getUpdateMetrics(): UpdateMetricsCollector? {
            return metrics?.updateMetricsCollector
        }
    }

    private fun setMetrics(metricsCollector: MetricsCollector) {
        metrics = metricsCollector
    }

    data class InitialUpdateParams(
        val update: SoftwareUpdate, val atInstant: Int, val initialPosition: List<Server>
    )

    data class SimulationParams(
        val network: Network, val updatesParams: List<InitialUpdateParams>
    )

    // todo: logToConsole and collectMetrics as parameters
    data class SimConfigParams(
        val maxSimDuration: Int? = 50000
    )

    fun runSimulation(
        // since I specify all network parameters here the topology is static as well as the update schedule
        // todo: following as class params
    ) {
        // initialize
        setMetrics(
            MetricsCollector("simulator metrics", params.network.edges, params.network.servers, params.updatesParams)
        )

        params.network.initNetwork()
        processInitialUpdates(params.updatesParams)

        // main loop
        while (true) {
            if (callbacks.isEmpty() || getCurrentTimestamp() > simConfigParams.maxSimDuration!!) {
                break
            }
            val currentCallback = callbacks.poll()

            setTimestamp(currentCallback.atInstant)
            currentCallback.runCallback()
        }

        // cleanup
        metrics?.printAndGetGraph()
    }

    private fun processInitialUpdates(updates: List<InitialUpdateParams>) {
        updates.stream().forEach {
            for (position in it.initialPosition) {
                val p = UpdatePackage(
                    position,
                    position,
                    it.update.size + params.network.networkConfig.packageOverhead,
                    it.update
                )
                val initUpdateCallback = TimedCallback(it.atInstant) {
                    position.initializeUpdate(p)
                }
                callbacks.add(initUpdateCallback)
            }
        }
    }
}
