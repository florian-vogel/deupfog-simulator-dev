import java.time.Instant
import java.util.PriorityQueue

data class Strategy(val name: String, val callbackOnCreate: () -> TimedCallback)

data class Link(val id: Number, val from: Node, val to: Node, val strategy: Strategy)
data class Node(val id: Number, var links: List<Link>)
data class Package(val id: Number)
data class Network(val nodes: List<Node>, val packages: List<Package>)
data class MetricsCollector(val name: String)
data class SimulationModel(val network: Network)
data class TimedCallback(val instant: Instant, val callback: () -> TimedCallback?)

class Simulator (val network: Network, val metricsCollector: MetricsCollector){
    // static state ? -> access state (Model) variable from everywhere
    val currentState: SimulationModel = SimulationModel(Network(emptyList(), emptyList()));
    val callbacks: PriorityQueue<TimedCallback> = PriorityQueue {
        c1, c2 -> c1.instant.compareTo(c2.instant)
    }

    fun runSimulation(){
        // abarbeiten der callback queue
        while(true){
            if (callbacks.isEmpty()){
                break;
            }

            var currentCallback = callbacks.peek();
            currentCallback.callback();
        }
    }
}

fun main() {
    val push = Strategy("push") { TimedCallback(Instant.EPOCH) {  null}}
    val n0 = Node(0, emptyList())
    val n1 = Node(1, emptyList())
    val l0 = Link(0, n0, n1, push)
    n0.links = listOf(l0)
    n1.links = listOf(l0)
    val nodes = listOf(n0, n1)
    val packages = emptyList<Package>()
    val network = Network(nodes, packages)
    val metrics = MetricsCollector("m1")
    val sim = Simulator(network, metrics)

    sim.runSimulation();
}