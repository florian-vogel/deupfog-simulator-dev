import java.util.*
import kotlin.collections.HashMap

open class Node(private val links: LinkedList<UnidirectionalLink>, private val maxElements: Int) {

    open fun receive(p: Package) {
    }

    fun arrivedVia(link: UnidirectionalLink) {
        link.removeFirst()
    }

    fun addLink(link: UnidirectionalLink) {
        this.links.add(link)
    }

    fun getLinks(): List<UnidirectionalLink> {
        return this.links
    }

    private fun removeFromQueue(queue: Queue<Package>) {
        queue.poll()
    }

    private fun passToQueue(destination: Node, p: Package) {
        getLinkTo(destination)!!.lineUpPackage(p)
    }


    fun getLinkTo(node: Node): UnidirectionalLink? {
        return links.find { it.getDestination() === node }
    }


    private fun elementsAtNode(): Int {
        return this.links.sumOf { it.queue.size }
    }
}

data class Software(val name: String)

// TODO: unterscheiden zwischen software version und software update (updates erlauben erhöhung der version)
data class SoftwareVersion(val target: Software, val versionNumber: Int, override val size: Int) : PackagePayload(size)

class ServerNode(
    links: LinkedList<UnidirectionalLink>, maxElements: Int, private val responsibleUpdateServer: ServerNode? = null
) : Node(links, maxElements) {
    class SoftwareUpdateRegistry() {
        private val registry = mutableMapOf<Software, SoftwareVersion>()

        fun updateIfMoreRecent(software: Software, update: SoftwareVersion) {
            val currentUpdate = registry[software]
            if (currentUpdate == null || update.versionNumber > currentUpdate.versionNumber) registry[software] = update
        }

        fun getCurrentVersion(software: Software): SoftwareVersion? {
            return registry[software]
        }
    }

    private val updateRegistry = SoftwareUpdateRegistry()
    private val edgeRegistry = mutableMapOf<Software, LinkedList<EdgeNode>>()
    private val serverRegistry: LinkedList<ServerNode> = LinkedList()

    init {
        registerAtServer()
    }

    private fun registerAtServer() {
        responsibleUpdateServer?.registerNodeInRegistry(this)
    }

    override fun receive(p: Package) {
        super.receive(p)
        if (p is UpdatePackage) {
            updateRegistry.updateIfMoreRecent(p.getUpdate().target, p.getUpdate())
            notifyLinksAboutNewUpdate(p.getUpdate())
        }
        if (p is RequestPackage) {
            getLinks().filter { it.getDestination() == p.getInitialPosition() }.forEach { it.tryTransfer() }
        }
    }

    fun registerNodeInRegistry(node: Node) {
        if (node is EdgeNode) {
            val software = node.runningSoftware.map { it.key }
            software.forEach {
                val targets = edgeRegistry[it]
                if (targets !== null) {
                    targets.add(node)
                } else {
                    edgeRegistry[it] = LinkedList<EdgeNode>()
                    edgeRegistry[it]?.add(node)
                }
            }
        } else if (node is ServerNode) {
            serverRegistry.add(node)
        }
    }

    private fun notifyLinksAboutNewUpdate(update: SoftwareVersion) {
        // TODO: eventuell nochmal umschreiben, schlecht lesbar und komische struktur
        getLinks().forEach {
            if (it.getDestination() is ServerNode || this.edgeRegistry[update.target]?.contains(it.getDestination()) == true) {
                val newUpdatePackage = UpdatePackage(this, update, "")
                it.lineUpPackage(newUpdatePackage)
            }
        }
    }
}

class EdgeNode(
    links: LinkedList<UnidirectionalLink>,
    maxElements: Int,
    val runningSoftware: HashMap<Software, SoftwareVersion>,
    private val responsibleUpdateServer: ServerNode,
) : Node(links, maxElements) {

    init {
        registerAtServer()
        Simulator.metrics.updateMetricsCollector.registerEdge(this)
    }

    private fun registerAtServer() {
        responsibleUpdateServer.registerNodeInRegistry(this)
    }

    override fun receive(p: Package) {
        if (p is UpdatePackage) {
            applyUpdate(p)
            Simulator.metrics.updateMetricsCollector.onUpdateArrive(this, p.getUpdate())
        }
        super.receive(p)
        // TODO: notify metricscollector
    }

    // TODO: oder als eigenen callback? hier wird software installation time vernachlässsigt
    private fun applyUpdate(p: UpdatePackage) {
        val currentVersion = this.runningSoftware[p.getUpdate().target]?.versionNumber
        if (currentVersion === null || currentVersion < p.getUpdate().versionNumber) {
            runningSoftware[p.getUpdate().target] = p.getUpdate()
            println("received new update: ${p.getUpdate().versionNumber}, for software: ${p.getUpdate().target.name}")
        }
    }
}
