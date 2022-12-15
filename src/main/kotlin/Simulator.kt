import java.util.PriorityQueue

class Simulator() {
    companion object {
        private var currentTimestamp: Int = 0
        private val callbacks: PriorityQueue<TimedCallback> = PriorityQueue { c1, c2 ->
            c1.atInstant.compareTo(c2.atInstant)
        }
        var metrics: MetricsCollector? = null
        val simulationName = "Simulation01"

        fun addCallback(c: TimedCallback) {
            this.callbacks.add(c)
        }

        fun cancelCallback(c: TimedCallback) {
            this.callbacks.remove(c)
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
        val update: SoftwareUpdate, val atInstant: Int, val initialPosition: UpdateReceiverNode
    )

    data class SimulationParams(
        val edges: List<Edge>, val servers: List<Server>, val updatesParams: List<InitialUpdateParams>
    )

    data class SimConfigParams(
        val nodesStartOnline: Boolean = true,
    )

    fun runSimulation(
        // since I specify all network parameters here the topology is static as well as the update schedule
        params: SimulationParams,
        simConfigParams: SimConfigParams
    ) {
        // initialize
        setMetrics(
            MetricsCollector("simulator metrics", params.edges, params.servers, params.updatesParams)
        )

        if (simConfigParams.nodesStartOnline) {
            params.edges.forEach {
                it.changeOnlineState(true) //; it.getLinks().forEach { link -> link.changeOnlineState(true) }
            }
            params.servers.forEach {
                it.changeOnlineState(true) //; it.getLinks().forEach { link -> link.changeOnlineState(true) }
            }
        }

        processInitialUpdates(params.updatesParams)

        // main loop
        while (true) {
            if (callbacks.isEmpty() || getCurrentTimestamp() > 1000) {
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
            val p = UpdatePackage(it.initialPosition, it.initialPosition, 1, it.update)
            val initUpdateCallback = TimedCallback(it.atInstant) {
                if (!it.initialPosition.listeningFor().map { it.type }.contains(it.update.type))
                    throw Exception("update cannot be initialized at node")
                p.initialPosition.receive(p)
            }
            callbacks.add(initUpdateCallback)
        }
    }
}
