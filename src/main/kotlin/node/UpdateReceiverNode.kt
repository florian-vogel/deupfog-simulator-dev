package node

import PullLatestUpdatesRequest
import RegisterForUpdatesRequest
import TimedCallback
import UpdatePackage
import Package
import findShortestPath
import main.Simulator
import software.SoftwareState
import software.SoftwareUpdate

data class UpdateRetrievalParams(
    // Push
    val registerAtServerForUpdates: Boolean = false,
    // Pull
    val updateRequestInterval: Int? = null
)

abstract class UpdateReceiverNode(
    nodeSimParams: NodeSimParams,
    private val responsibleServers: List<Server>,
    // either increase version of registered nodes implicitly in server or send updateReceived notification (updates runningSoftware states) when edge received update
    // --> currently sending update received notification
    private val runningSoftware: List<SoftwareState>,
    private val updateRetrievalParams: UpdateRetrievalParams,
    initialNodeState: MutableNodeState
) : Node(nodeSimParams, initialNodeState) {
    private val currentNodeState = initialNodeState
    private var pullRequestSchedule: TimedCallback? = null

    override fun initNode() {
        if (currentNodeState.online) {
            initStrategy()
        }
    }

    override fun receive(p: Package) {
        if (!getOnlineState()) return
        if ((p is UpdatePackage) && (p.destination == this)) {
            processUpdate(p.update)
        } else {
            addToPackageQueue(p)
        }
    }

    open fun processUpdate(update: SoftwareUpdate) {
        updateRunningSoftware(update)
        Simulator.getUpdateMetrics()?.onArrive(update, this)
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
            responsibleServers.forEach {
                registerAtServer(it, listeningFor)
            }
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

    private fun registerAtServer(server: Server, listeningFor: List<SoftwareState>) {
        if (updateRetrievalParams.registerAtServerForUpdates && listeningFor.isNotEmpty()) {
            val requestPackageOverhead = 1
            val request = RegisterForUpdatesRequest(requestPackageOverhead, this, server, listeningFor)
            val nextHop = findShortestPath(this, server)?.peek()
            if (nextHop != null) {
                receive(request)
            }
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
        responsibleServers.forEach {
            val requestPackageOverhead = 1
            val request = PullLatestUpdatesRequest(requestPackageOverhead, this, it, listeningFor())
            val nextHop = findShortestPath(this, it)?.peek()
            if (nextHop != null) {
                receive(request)
            }
        }
    }
}

