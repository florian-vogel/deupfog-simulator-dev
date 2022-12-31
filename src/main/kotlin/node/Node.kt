package node

import network.LinkConfig
import network.MutableLinkState
import network.UnidirectionalLink
import java.util.LinkedList
import Package
import main.Simulator
import java.util.Queue

open class MutableNodeState(
    val online: Boolean,
)

open class NodeConfig(
    val storageCapacity: Int, val nextOnlineStateChange: ((current: Int, online: Boolean) -> Int?)? = null,
    // todo
    val calculateProcessingTime: ((p: Package) -> Int)? = null
)

open class Node(
    private val simParams: NodeConfig, initialNodeState: MutableNodeState
) : OnlineState(initialNodeState.online, simParams.nextOnlineStateChange) {
    protected val links: MutableList<UnidirectionalLink> = mutableListOf()
    private val packageQueue: Queue<Package> = LinkedList()

    open fun initNode() {}

    open fun receive(p: Package) {
        if (!getOnlineState()) return
        if (p.destination != this) {
            addToPackageQueue(p)
        }
    }

    override fun changeOnlineState(value: Boolean) {
        super.changeOnlineState(value)
        Simulator.metrics?.nodeMetricsCollector?.onNodeStateChanged(this)
    }

    open fun createLink(linkConfig: LinkConfig, to: Node, initialLinkState: MutableLinkState): UnidirectionalLink {
        val newLink = UnidirectionalLink(linkConfig, this, to, initialLinkState)
        links.add(newLink)
        return newLink
    }

    open fun removeLink(link: UnidirectionalLink) {
        links.remove(link)
    }

    fun getOnlineLinks(): List<UnidirectionalLink>? {
        // todo: remove online check of node?
        if (!getOnlineState()) return null
        return this.links.filter { it.getOnlineState() }
    }

    fun getOnlineLinkTo(node: Node): UnidirectionalLink? {
        if (!getOnlineState()) return null
        return links.find { it.to == node && it.getOnlineState() }
    }

    protected fun addToPackageQueue(p: Package) {
        this.packageQueue.add(p)
        links.find { it.to == p.destination }?.startTransmission(p)
    }

    private fun getFreeStorageCapacity(): Int {
        return this.simParams.storageCapacity - this.packageQueue.sumOf { it.getSize() }
    }


    fun getNextPackage(link: UnidirectionalLink): Package? {
        return packageQueue.find { it.destination == link.to }
    }

    fun removePackagesWithoutPossibleRoute() {
        val queueCopy = packageQueue.toList()
        queueCopy.forEach {
            val noOnlineLink =
                links.find { link -> link.to == it.destination && link.getOnlineState() } == null
            if (noOnlineLink) {
                removePackage(it, true)
            }
        }
    }

    // todo:
    // call when we notice that a node is offline (?)
    fun removePackagesToNode(){}

    fun removePackage(p: Package, lost: Boolean = false) {
        this.packageQueue.remove(p)
        if (lost) {
            Simulator.metrics!!.nodeMetricsCollector.onPackageLost(this)
        }
    }
}

