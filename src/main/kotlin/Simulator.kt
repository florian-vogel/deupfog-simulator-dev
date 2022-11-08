import java.util.LinkedList
import java.util.PriorityQueue

data class SimulationModel(var packages: LinkedList<Package>, var time: Int)

class Simulator() {
    companion object {
        private val currentState: SimulationModel = SimulationModel(LinkedList<Package>(), 0)
        private val callbacks: PriorityQueue<TimedCallback> = PriorityQueue { c1, c2 ->
            c1.atInstant.compareTo(c2.atInstant)
        }
        val metrics = MetricsCollector("metrics1")

        // TODO: only temporary
        var serverNode: ServerNode? = null

        fun setUpdateServerNode(node: ServerNode) {
            serverNode = node
        }

        fun addCallback(c: TimedCallback) {
            this.callbacks.add(c)
        }

        fun addPackage(p: Package) {
            currentState.packages.add(p)
        }

        fun getCurrentTimestamp(): Int {
            return this.currentState.time
        }

        fun setTimestamp(value: Int) {
            this.currentState.time = value
        }
    }

    // TODO: specify order for callbacks at the same timestep (package arrive before requestPackage arrive)
    fun runSimulation(
        initialTimedCallbacks: List<InitPackageCallback>?
    ) {
        processInitialTimedCallbacks(initialTimedCallbacks)

        while (true) {
            if (callbacks.isEmpty() || getCurrentTimestamp() > 300) {
                break
            }
            val currentCallback = callbacks.poll()

            setTimestamp(currentCallback.atInstant)
            currentCallback.runCallback()
        }
    }

    private fun processInitialTimedCallbacks(initialTimedCallbacks: List<InitPackageCallback>?) {
        if (initialTimedCallbacks !== null) {
            initialTimedCallbacks.stream().forEach {
                callbacks.add(it)
            }
        }
    }

}
