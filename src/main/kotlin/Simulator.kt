import java.util.PriorityQueue

class Simulator() {
    companion object {
        private var currentTimestamp: Int = 0
        private val callbacks: PriorityQueue<TimedCallback> = PriorityQueue { c1, c2 ->
            c1.atInstant.compareTo(c2.atInstant)
        }
        val metrics = MetricsCollector("metrics1")

        fun addCallback(c: TimedCallback) {
            this.callbacks.add(c)
        }

        fun getCurrentTimestamp(): Int {
            return currentTimestamp
        }

        fun setTimestamp(value: Int) {
            currentTimestamp = value
        }
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
