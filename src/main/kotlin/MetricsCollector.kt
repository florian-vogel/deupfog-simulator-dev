import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

class MetricsCollector(val name: String) {
    // val linkMetricsCollector = LinkMetricsCollector()
    val updateMetricsCollector = UpdateMetricsCollector()
    // private val edgeRegistry = mutableMapOf<Software, LinkedList<EdgeNode>>()
    // private val serverRegistry: LinkedList<ServerNode> = LinkedList()

    fun printMetrics() {
        // this.linkMetricsCollector.printMetrics()
        this.updateMetricsCollector.printMetrics()
    }
}

class LinkMetricsCollector() {
    class MetricsPerLink() {
        var timeOccupied: Int = 0
        var packagesWaitingOnAvarage: Float = 0f
        var packagesLost: Int = 0
        // TODO: implement behaviour for above vars

        private var isTransferring: Boolean = false
        private var timestampAtLastIsTransferringChange = Simulator.getCurrentTimestamp()

        fun onStartTransfer() {
            if (!isTransferring) {
                isTransferring = true
                timestampAtLastIsTransferringChange = Simulator.getCurrentTimestamp()
            }
        }

        fun onStopTransfer() {
            if (isTransferring) {
                isTransferring = false
                timeOccupied += Simulator.getCurrentTimestamp() - timestampAtLastIsTransferringChange
                timestampAtLastIsTransferringChange = Simulator.getCurrentTimestamp()
            }
        }
    }

    private val metrics: MutableMap<UnidirectionalLink, MetricsPerLink> = LinkedHashMap()

    fun register(link: UnidirectionalLink) {
        metrics[link] = MetricsPerLink()
    }

    fun getMetricsCollectorForLink(link: UnidirectionalLink): MetricsPerLink? {
        return metrics[link]
    }

    fun printMetrics() {
        this.metrics.forEach {
            println("link: ${it.key}, timeOccupied: ${it.value.timeOccupied}")
        }
    }
}

class UpdateMetricsCollector(
    val printTimePerNode: Boolean = false
) {
    class MetricsPerUpdate(private val edgesWaiting: LinkedList<EdgeNode>) {
        var createTimestamp = Simulator.getCurrentTimestamp()
        var timeTillUpdateReachedEveryEdge: Int? = null
        var arriveTimestampPerEdge: HashMap<EdgeNode, Int?> = HashMap()

        fun onUpdateArriveAtEdge(edgeNode: EdgeNode) {
            edgesWaiting.remove(edgeNode)
            arriveTimestampPerEdge[edgeNode] = Simulator.getCurrentTimestamp() - createTimestamp
            if (edgesWaiting.isEmpty()) {
                timeTillUpdateReachedEveryEdge = Simulator.getCurrentTimestamp() - createTimestamp
            }
        }

        fun newNode(edge: EdgeNode) {
            this.edgesWaiting.add(edge)
        }
    }

    private val metrics: MutableMap<SoftwareVersion, MetricsPerUpdate> = LinkedHashMap()
    private val edgeRegistry: MutableMap<Software, LinkedList<EdgeNode>> = hashMapOf()

    fun registerUpdate(update: SoftwareVersion) {
        if (metrics[update] == null) {
            val edgesWaiting = this.edgeRegistry[update.target]
            metrics[update] = MetricsPerUpdate(edgesWaiting!!)
        }
    }

    // TODO: duplication with serverNode
    // keep registry as own type
    fun registerEdge(edge: EdgeNode) {
        val software = edge.runningSoftware.map { it.key }
        software.forEach { software ->
            val targets = edgeRegistry[software]
            if (targets !== null) {
                targets.add(edge)
            } else {
                edgeRegistry[software] = LinkedList<EdgeNode>()
                edgeRegistry[software]?.add(edge)
            }
            val filteredMetrics = metrics.filter { (key, value) -> key.target === software }
            filteredMetrics.entries.forEach { it.value.newNode(edge) }
        }
    }

    fun onUpdateArrive(node: Node, update: SoftwareVersion) {
        if (node is EdgeNode) {
            metrics[update]?.onUpdateArriveAtEdge(node)
        }
    }

    fun printMetrics() {
        this.metrics.forEach {
            var timePerNode = ""
            if (printTimePerNode) {
                timePerNode += "timePerNode: TODO"
            }
            println(
                "update: ${it.key}, " + "timetillUpdateReachedEveryEdge: ${it.value.timeTillUpdateReachedEveryEdge}, " + "createTimestamp: ${it.value.createTimestamp}, " + timePerNode
            )
        }
    }
}
