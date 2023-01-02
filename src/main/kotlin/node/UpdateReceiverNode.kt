package node

import PullLatestUpdatesRequest
import RegisterForUpdatesRequest
import TimedCallback
import UpdatePackage
import Package
import simulator.Simulator
import software.SoftwareState
import software.SoftwareUpdate

data class UpdateRetrievalParams(
    // Push
    val registerAtServerForUpdates: Boolean = false,
    // Pull
    val updateRequestInterval: Int? = null,
)

open class PackageConfig(
    val registerRequestOverhead: Int,
    val pullRequestOverhead: Int,
)

abstract class UpdateReceiverNode(
    nodeConfig: NodeConfig,
    private val assignedServers: List<Server>,
    private val runningSoftware: List<SoftwareState>,
    private val updateRetrievalParams: UpdateRetrievalParams,
    initialNodeState: MutableNodeState,
    private val packageConfig: PackageConfig,
) : Node(nodeConfig, initialNodeState) {

    private var pullRequestSchedule: TimedCallback? = null

    override fun initNode() {
        if (getOnlineState()) {
            initStrategy()
        }
    }

    override fun receive(p: Package) {
        if (getOnlineState()) {
            if ((p is UpdatePackage) && (p.destination == this)) {
                processUpdate(p.update)
            } else {
                addToPackageQueue(p)
            }
        }
    }

    open fun processUpdate(update: SoftwareUpdate) {
        updateRunningSoftware(update)
        Simulator.getMetrics()?.updateMetricsCollector?.onArrive(update, this)
    }

    override fun changeOnlineState(value: Boolean) {
        val onlineStatusChanged = value != getOnlineState()
        if (onlineStatusChanged) {
            super.changeOnlineState(value)
            if (value) {
                initStrategy()
            } else {
                cancelStrategy()
            }
        }
    }

    private fun initStrategy() {
        registerAtServers(listeningFor())
        makePullRequestsRecursive()
    }

    protected fun registerAtServers(listeningFor: List<SoftwareState>) {
        if (updateRetrievalParams.registerAtServerForUpdates) {
            assignedServers.forEach {
                registerAtServer(it, listeningFor)
            }
        }
    }

    private fun registerAtServer(server: Server, listeningFor: List<SoftwareState>) {
        if (updateRetrievalParams.registerAtServerForUpdates && listeningFor.isNotEmpty()) {
            val packageOverhead = packageConfig.registerRequestOverhead
            val registerRequest = RegisterForUpdatesRequest(packageOverhead, this, server, listeningFor)
            receive(registerRequest)
        }
    }

    private fun cancelStrategy() {
        removePullRequestSchedule()
    }

    open fun listeningFor(): List<SoftwareState> {
        return runningSoftware
    }


    private fun updateRunningSoftware(update: SoftwareUpdate) {
        val targetSoftware = runningSoftware::find { it.type == update.type }
        if (targetSoftware != null) {
            targetSoftware.applyUpdate(update)
            registerAtServers(listeningFor())
        }
    }

    private fun removePullRequestSchedule() {
        if (pullRequestSchedule != null) {
            Simulator.cancelCallback(pullRequestSchedule!!)
        }
        pullRequestSchedule = null
    }

    private fun makePullRequestsRecursive() {
        if (updateRetrievalParams.updateRequestInterval != null) {
            sendPullRequestsToResponsibleServers()
            schedulePullRequest(updateRetrievalParams.updateRequestInterval)
        }
    }

    private fun schedulePullRequest(nextRequestIn: Int) {
        pullRequestSchedule = TimedCallback(Simulator.getCurrentTimestamp() + nextRequestIn) {
            makePullRequestsRecursive()
        }
        Simulator.addCallback(pullRequestSchedule!!)
    }

    private fun sendPullRequestsToResponsibleServers() {
        assignedServers.forEach {
            val packageOverhead = packageConfig.pullRequestOverhead
            val request = PullLatestUpdatesRequest(packageOverhead, this, it, listeningFor())
            receive(request)
        }
    }
}

