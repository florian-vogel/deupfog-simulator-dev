package node

import PullLatestUpdatesRequest
import RegisterForUpdatesRequest
import simulator.TimedCallback
import UpdatePackage
import Package
import simulator.Simulator
import software.SoftwareState
import software.SoftwareUpdate

data class UpdateRetrievalParams(
    // Push
    val registerAtServerForUpdates: Boolean = false,
    // Pull
    val chooseNextUpdateRequestInterval: (() -> Int)? = null,
)

open class PackagesConfig(
    val registerRequestOverhead: Int,
    val pullRequestOverhead: Int,
    val calculatePackageSendProcessingTime: ((p: Package, numOfPackagesInQueue: Int) -> Int) = { _, _ -> 0 }
)

abstract class UpdateReceiverNode(
    nodeConfig: NodeConfig,
    private val assignedServers: List<Server>,
    protected val runningSoftware: List<SoftwareState>,
    private val updateRetrievalParams: UpdateRetrievalParams,
    initialNodeState: MutableNodeState,
    private val packagesConfig: PackagesConfig,
) : Node(nodeConfig, initialNodeState) {

    private var pullRequestSchedule: TimedCallback? = null

    override fun initNode() {
        if (getOnlineState()) {
            initStrategy()
        }
    }

    override fun receive(p: Package) {
        if (getOnlineState()) {
            if (p is UpdatePackage) {
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
            val packageOverhead = packagesConfig.registerRequestOverhead
            val registerRequest = RegisterForUpdatesRequest(packageOverhead, this, server, listeningFor)
            initPackage(registerRequest)
        }
    }

    private fun cancelStrategy() {
        removePullRequestSchedule()
    }

    open fun listeningFor(): List<SoftwareState> {
        val copy = runningSoftware
        return copy.map { SoftwareState(it.type, it.versionNumber, it.size) }
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
        val chooseNextUpdateRequestInterval = updateRetrievalParams.chooseNextUpdateRequestInterval;
        if (chooseNextUpdateRequestInterval != null) {
            sendPullRequestsToResponsibleServers()
            schedulePullRequest(chooseNextUpdateRequestInterval())
        }
    }

    private fun schedulePullRequest(nextRequestIn: Int) {
        val newPullRequestSchedule = TimedCallback(Simulator.getCurrentTimestamp() + nextRequestIn) {
            makePullRequestsRecursive()
        }
        Simulator.addCallback(newPullRequestSchedule)
        pullRequestSchedule = newPullRequestSchedule
    }

    private fun sendPullRequestsToResponsibleServers() {
        assignedServers.forEach {
            val packageOverhead = packagesConfig.pullRequestOverhead
            val request = PullLatestUpdatesRequest(packageOverhead, this, it, listeningFor())
            initPackage(request)
        }
    }

    protected fun initPackage(p: Package) {
        val processingTime = packagesConfig.calculatePackageSendProcessingTime(
            p,
            countPackagesInQueue()
        )
        val initPackageCallback = TimedCallback(
            Simulator.getCurrentTimestamp() + processingTime
        ) {
            receive(p)
        }

        Simulator.getMetrics()?.resourcesUsageMetricsCollector?.onProcessPackage(
            processingTime
        )
        Simulator.addCallback(
            initPackageCallback
        )
    }
}

