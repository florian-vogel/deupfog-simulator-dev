import java.util.PriorityQueue

class Simulator() {
    companion object {
        private var currentTimestamp: Int = 0
        private val callbacks: PriorityQueue<TimedCallback> = PriorityQueue { c1, c2 ->
            c1.atInstant.compareTo(c2.atInstant)
        }
        var metrics: MetricsCollector? = null

        fun addCallback(c: TimedCallback) {
            this.callbacks.add(c)
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
        val update: SoftwareUpdate, val atInstant: Int, val initialPosition: Node
    )

    data class SimulationParams(
        val edges: List<Edge>, val servers: List<Server>, val updatesParams: List<InitialUpdateParams>
    )

    // TODO: specify order for callbacks at the same timestep (package arrive before requestPackage arrive)
    fun runSimulation(
        // since I specify all network parameters here the topology is static as well as the update schedule
        params: SimulationParams
    ) {
        // initialize
        setMetrics(
            MetricsCollector("simulator metrics", params.edges, params.servers, params.updatesParams.map { it.update })
        )
        processInitialUpdates(params.updatesParams)
        params.edges.forEach { it.setOnline(true) }
        params.servers.forEach { it.setOnline(true) }

        // main loop
        while (true) {
            if (callbacks.isEmpty() || getCurrentTimestamp() > 300) {
                break
            }
            val currentCallback = callbacks.poll()

            setTimestamp(currentCallback.atInstant)
            currentCallback.runCallback()
        }

        // cleanup
        metrics?.printMetrics()
    }

    private fun processInitialUpdates(updates: List<InitialUpdateParams>) {
        updates.stream().forEach {
            val p = UpdatePackage(it.initialPosition, it.initialPosition, 1, it.update)
            val initUpdateCallback = InitPackageAtNodeCallback(it.atInstant, p)
            callbacks.add(initUpdateCallback)
        }
    }
}
