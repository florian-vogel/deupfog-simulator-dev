package node

import network.*
import simulator.TimedCallback
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
    protected val runningSoftware: MutableList<SoftwareState>,
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
        if (!getOnlineState()) return
        if (p.destination == this) {
            if (p is UpdatePackage) {
                processUpdate(p.update)
            }
        } else {
            addToPackageQueue(p)
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
            println("${Simulator.getCurrentTimestamp()} online state change: $this, $value")
            if (value) {
                initStrategy()
            } else {
                cancelStrategy()
            }
        }
    }

    private fun initStrategy() {
        registerAtServers(softwareInformation())
        makePullRequestsRecursive()
    }

    protected fun registerAtServers(softwareInformation: SoftwareInformation) {
        if (updateRetrievalParams.registerAtServerForUpdates) {
            assignedServers.forEach {
                registerAtServer(it, softwareInformation)
            }
        }
    }

    private fun registerAtServer(server: Server, softwareInformation: SoftwareInformation) {
        if (updateRetrievalParams.registerAtServerForUpdates) {
            val packageOverhead = packagesConfig.registerRequestOverhead
            val registerRequest = RegisterForUpdatesRequest(packageOverhead, this, server, softwareInformation)
            initPackage(registerRequest)
        }
    }

    private fun cancelStrategy() {
        removePullRequestSchedule()
    }

    open fun softwareInformation(): SoftwareInformation {
        val runningSoftwareCopy = runningSoftware.toList()
        return SoftwareInformation(runningSoftwareCopy)
    }


    private fun updateRunningSoftware(update: SoftwareUpdate) {
        val updateTarget = runningSoftware::find { it.type == update.type }
        if (updateTarget != null) {
            runningSoftware.remove(updateTarget)
            runningSoftware.add(updateTarget.applyUpdate(update))
            registerAtServers(softwareInformation())
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
            val request = PullLatestUpdatesRequest(packageOverhead, this, it, softwareInformation())
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
        Simulator.addCallback(
            initPackageCallback
        )

        Simulator.getMetrics()?.resourcesUsageMetricsCollector?.onProcessPackage(
            processingTime
        )
    }
}

