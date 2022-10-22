import java.util.LinkedList
import java.util.PriorityQueue

data class SimulationModel(var network: Network, var packages: LinkedList<Package>, var time: Int)

class Simulator() {
    companion object {
        private val currentState: SimulationModel = SimulationModel(Network(), LinkedList<Package>(), 0)
        private val callbacks: PriorityQueue<PackageStateChangeCallback> = PriorityQueue { c1, c2 ->
            c1.atInstant.compareTo(c2.atInstant)
        }

        fun addCallback(c: PackageStateChangeCallback) {
            this.callbacks.add(c)
        }

        fun addPackage(p: Package) {
            currentState.packages.add(p)
        }

        fun getCurrentTimestamp(): Int {
            return this.currentState.time
        }

        fun findNextHop(p: Package): Node? {
            val shortestPath = currentState.network.findShortestPath(p.getPosition(), p.getPosition())
            return if (shortestPath.isEmpty()) {
                null
            } else {
                shortestPath.first
            }
        }
    }

    fun runSimulation(network: Network, initialTimedCallbacks: List<InitPackageCallback>?) {
        setNetwork(network)
        processInitialTimedCallbacks(initialTimedCallbacks)

        while (true) {
            if (callbacks.isEmpty()) {
                break
            }

            val currentCallback = callbacks.poll()
            currentCallback.runCallback()
        }
    }

    private fun setNetwork(network: Network) {
        currentState.network = network
    }

    private fun processInitialTimedCallbacks(initialTimedCallbacks: List<InitPackageCallback>?) {
        if (initialTimedCallbacks !== null) {
            initialTimedCallbacks.stream().forEach {
                callbacks.add(it)
            }
        }
    }

}
