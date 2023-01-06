package node

import network.LinkConfig
import network.MutableLinkState
import network.UnidirectionalLink
import java.util.LinkedList
import network.Package
import simulator.Simulator
import java.util.Queue

open class MutableNodeState(
    val online: Boolean,
)

open class NodeConfig(
    val storageCapacity: Int, val nextOnlineStateChange: ((current: Int, online: Boolean) -> Int?)? = null,
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

    open fun createLink(linkConfig: LinkConfig, to: Node, initialLinkState: MutableLinkState): UnidirectionalLink {
        val newLink = UnidirectionalLink(linkConfig, this, to, initialLinkState)
        links.add(newLink)
        return newLink
    }

    open fun removeLink(link: UnidirectionalLink) {
        links.remove(link)
    }

    fun getOnlineLinks(): List<UnidirectionalLink>? {
        return this.links.filter { it.getOnlineState() }
    }

    fun getOnlineLinkTo(node: Node): UnidirectionalLink? {
        return links.find { it.to == node && it.getOnlineState() }
    }

    fun getNextPackage(link: UnidirectionalLink): Package? {
        return packageQueue.find { it.destination == link.to }
    }

    fun checkAndRemovePackagesWithoutRoutingOptions() {
        val queueCopy = packageQueue.toList()
        queueCopy.forEach {
            val noOnlineLink =
                links.find { link -> link.to == it.destination && link.getOnlineState() } == null
            val destinationOffline = !it.destination.getOnlineState()
            if (noOnlineLink || destinationOffline) {
                removePackage(it, true)
            }
        }
    }

    fun removePackage(p: Package, lost: Boolean = false) {
        this.packageQueue.remove(p)
        if (lost) {
            Simulator.getMetrics()?.packageLossMetricsCollector?.onPackageLost()
        }
    }

    protected fun addToPackageQueue(p: Package) {
        if (this.freeStorageCapacity() > p.getSize()) {
            this.packageQueue.add(p)
            links.find { it.to == p.destination }?.startTransmission(p)
        } else {
            Simulator.getMetrics()?.packageLossMetricsCollector?.onPackageLost()
        }
    }

    private fun freeStorageCapacity(): Int {
        return this.simParams.storageCapacity - this.packageQueue.sumOf { it.getSize() }
    }

    protected fun countPackagesInQueue(): Int {
        return this.packageQueue.size
    }
}

