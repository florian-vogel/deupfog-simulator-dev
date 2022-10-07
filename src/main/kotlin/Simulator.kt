import java.util.LinkedList
import java.util.PriorityQueue

data class SimulationModel(var network: Network, var packages: LinkedList<Package>, var time: Int)

class Simulator(private val initialTimedCallbacks: List<InitPackageCallback>?) {
    companion object {
        // nur die callbacks sollten static sein, der rest wird dem callback beim Aufruf mitgegeben
        val currentState: SimulationModel = SimulationModel(Network(LinkedList()), LinkedList<Package>(), 0)
        val callbacks: PriorityQueue<PackageStateChangeCallback> = PriorityQueue { c1, c2 ->
            c1.atInstant.compareTo(c2.atInstant)
        }

        fun setNetwork(n: Network) {
            currentState.network = n
        }

        fun setPackages(p: LinkedList<Package>) {
            currentState.packages = p
        }

        fun findNextHop(p: Package): PackagePosition? {
            val shortestPath = currentState.network.findShortestPath(p.getPosition(), p.getPosition())
            return if (shortestPath.isEmpty()) {
                null
            } else {
                shortestPath.first
            }
        }
    }

    fun runSimulation() {
        processInitialTimedCallbacks()

        while (true) {
            if (callbacks.isEmpty()) {
                break
            }

            val currentCallback = callbacks.poll()
            currentCallback.runCallback()
        }
    }

    private fun processInitialTimedCallbacks() {
        if (initialTimedCallbacks !== null) {
            initialTimedCallbacks.stream().forEach {
                callbacks.add(it)
            }
        }
    }

}
