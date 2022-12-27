package node

import UnidirectionalLink
import findShortestPath
import java.util.LinkedList
import Package
import main.Simulator

open class MutableNodeState(
    val online: Boolean,
)

open class NodeSimParams(
    val storageCapacity: Int,
    val nextOnlineStateChange: ((current: Int, online: Boolean) -> Int?)? = null,
    // todo
    val calculateProcessingTime: ((p: Package) -> Int)? = null
)

open class Node(
    private val simParams: NodeSimParams, initialNodeState: MutableNodeState
) : OnlineState(initialNodeState.online, simParams.nextOnlineStateChange) {
    private val links: MutableList<UnidirectionalLink> = mutableListOf()
    private val packageQueue = LinkedList<Package>()

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

    open fun addLink(link: UnidirectionalLink) {
        links.add(link)
        link.initializeFromParams({ getNextPackage(it) }) { p -> this.removePackage(p) }
    }

    open fun removeLink(link: UnidirectionalLink) {
        links.remove(link)
    }

    fun getLinks(): List<UnidirectionalLink>? {
        if (!getOnlineState()) return null
        return this.links.filter { it.getOnlineState() }
    }

    fun getLinkTo(node: Node): UnidirectionalLink? {
        if (!getOnlineState()) return null
        return links.find { it.to == node && it.getOnlineState() }
    }

    protected fun addToPackageQueue(p: Package) {
        this.packageQueue.add(p)
        val nextHop = findShortestPath(this, p.destination)?.firstOrNull()
        if (nextHop != null) {
            links.find { it.to == nextHop }?.tryTransmission(p)
        }
    }

    private fun getFreeStorageCapacity(): Int {
        return this.simParams.storageCapacity - this.packageQueue.sumOf { it.getSize() }
    }


    private fun getNextPackage(link: UnidirectionalLink): Package? {
        val queueCopy = packageQueue.toList()
        queueCopy.forEach {
            val nextHop = findShortestPath(this, it.destination)?.firstOrNull()
            if (nextHop == null) {
                removePackage(it, true)
            } else if (nextHop == link.to) {
                return it
            }
        }
        return null
    }

    private fun removePackage(p: Package, lost: Boolean = false) {
        this.packageQueue.remove(p)
        if (lost) {
            Simulator.metrics!!.nodeMetricsCollector.onPackageLost(this)
        }
    }
}

