import java.util.PriorityQueue

class Simulator(val metricsCollector: MetricsCollector<out UpdatableType>) {
    companion object {
        private var currentTimestamp: Int = 0
        private val callbacks: PriorityQueue<TimedCallback> = PriorityQueue { c1, c2 ->
            c1.atInstant.compareTo(c2.atInstant)
        }
        var metrics: MetricsCollector<out UpdatableType>? = null

        fun addCallback(c: TimedCallback) {
            this.callbacks.add(c)
        }

        fun getCurrentTimestamp(): Int {
            return currentTimestamp
        }

        fun setTimestamp(value: Int) {
            currentTimestamp = value
        }

        fun getUpdateMetrics(): UpdateMetricsCollector<out UpdatableType>? {
            return metrics?.updateMetricsCollector
        }
    }

    fun setMetrics(metricsCollector: MetricsCollector<out UpdatableType>) {
        metrics = metricsCollector
    }

    // TODO: specify order for callbacks at the same timestep (package arrive before requestPackage arrive)
    fun runSimulation(
        initialTimedCallbacks: List<InitPackageAtNodeCallback>?
    ) {
        if (initialTimedCallbacks !== null) {
            processInitialTimedCallbacks(initialTimedCallbacks)
        }
        while (true) {
            if (callbacks.isEmpty() || getCurrentTimestamp() > 300) {
                break
            }
            val currentCallback = callbacks.poll()

            setTimestamp(currentCallback.atInstant)
            currentCallback.runCallback()
        }
    }

    private fun processInitialTimedCallbacks(initialTimedCallbacks: List<InitPackageAtNodeCallback>) {
        initialTimedCallbacks.stream().forEach {
            callbacks.add(it)
        }
    }
}
