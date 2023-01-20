package network

import node.*
import software.Software
import software.SoftwareState

val createPushStrategy = { UpdateRetrievalParams(registerAtServerForUpdates = true) }
val createPullStrategy = { interval: Int ->
    UpdateRetrievalParams(
        chooseNextUpdateRequestInterval = ({ (1..interval).random() })
    )
}

val createRandomOfflineBehaviour =
    { avgTimeBetweenOfflineStates: Int, avgOfflineStateDuration: Int -> { currentTime: Int, online: Boolean -> if (online) currentTime + (1..avgTimeBetweenOfflineStates * 2).random() else currentTime + (1..avgOfflineStateDuration * 2).random() } }

data class NetworkConfig(
    val defaultUpdateRetrievalParams: UpdateRetrievalParams,
    val softwareTypes: List<Software> = listOf(),
    val defaultPackageConfig: PackagesConfigServer,
)

class Network(val networkConfig: NetworkConfig) {
    val edges = mutableListOf<Edge>()
    val servers = mutableListOf<Server>()
    val updateInitializationServers = mutableListOf<Server>()

    fun generateEdge(
        nodeSimParams: NodeConfig,
        responsibleUpdateServer: List<Server>,
        runningSoftware: MutableList<SoftwareState> = networkConfig.softwareTypes.map { SoftwareState(it, 0, 0) }
            .toMutableList(),
        updateRetrievalParams: UpdateRetrievalParams = networkConfig.defaultUpdateRetrievalParams,
        initialNodeState: MutableNodeState = MutableNodeState(true),
        packagesConfig: PackagesConfig = networkConfig.defaultPackageConfig
    ): Edge {
        val newEdge = Edge(
            nodeSimParams,
            responsibleUpdateServer,
            runningSoftware,
            updateRetrievalParams,
            initialNodeState,
            packagesConfig
        )
        edges.add(newEdge)
        return newEdge
    }

    fun generateServer(
        nodeSimParams: NodeConfig,
        responsibleUpdateServer: List<Server> = emptyList(),
        runningSoftware: MutableList<SoftwareState> = mutableListOf(),
        updateRetrievalParams: UpdateRetrievalParams = createPushStrategy(),
        initialNodeState: MutableNodeState = MutableNodeState(true),
        packageConfig: PackagesConfigServer = networkConfig.defaultPackageConfig
    ): Server {
        val newServer = Server(
            nodeSimParams,
            responsibleUpdateServer,
            runningSoftware,
            updateRetrievalParams,
            initialNodeState,
            packageConfig
        )
        servers.add(newServer)
        return newServer
    }

    fun initNetwork() {
        edges.forEach {
            it.initNode()
        }
        servers.forEach {
            it.initNode()
        }
    }
}

data class ClientServerConfiguration(
    val networkConfig: NetworkConfig,
    val serverSimParams: NodeConfig,
    val edgeCount: Int,
    val edgeSimParams: NodeConfig,
    val linkSimParams: () -> LinkConfig,
    val runningSoftware: List<SoftwareState>,
    val pullInterval: Int,
)

fun generateClientServerNetwork(config: ClientServerConfiguration): Network {
    val network = Network(config.networkConfig)
    val server = network.generateServer(
        config.serverSimParams, listOf(), mutableListOf(), UpdateRetrievalParams()
    )
    for (i in 0..config.edgeCount) {
        val edge = network.generateEdge(config.edgeSimParams, listOf(server),
            // todo: this map might be not nessasary -> same at hierarchyConfig
            config.runningSoftware.toList().map { SoftwareState(it.type, it.versionNumber, it.size) }
                .toMutableList())
        server.createLink(config.linkSimParams(), edge, MutableLinkState(true))
        edge.createLink(config.linkSimParams(), server, MutableLinkState(true))
    }
    network.updateInitializationServers += server
    return network
}


data class HierarchyConfiguration(
    val deepestLevel: Int,
    val branchingFactor: Int,
    val serverSimParamsAtLevel: (level: Int) -> NodeConfig,
    val serverServerLinkSimParamsAtLevel: (level: Int) -> LinkConfig,
)

data class EdgeGroupConfiguration(
    val runningSoftware: List<SoftwareState>,
    val updateRetrievalParams: UpdateRetrievalParams,
    val edgeSimParamsAtLevel: (level: Int) -> NodeConfig,
    val serverEdgeLinkSimParamsAtLevel: (level: Int) -> () -> LinkConfig,
    val edgesPerServerAtLevel: (level: Int) -> Int,
)

fun generateHierarchicalNetwork(
    networkConfig: NetworkConfig,
    hierarchyConfig: HierarchyConfiguration,
    edgeGroupConfigs: List<EdgeGroupConfiguration>
): Network {
    val network = Network(networkConfig)
    val serverHierarchy = generateServerHierarchy(network, hierarchyConfig)
    for (config in edgeGroupConfigs) {
        addEdgesToHierarchy(network, serverHierarchy, config)
    }
    network.updateInitializationServers += serverHierarchy[0] ?: emptyList()
    return network
}

fun generateServerHierarchy(network: Network, hierarchyConfig: HierarchyConfiguration): Map<Int, List<Server>> {
    val serversAtLevel = mutableMapOf<Int, List<Server>>()
    val rootServer = network.generateServer(
        hierarchyConfig.serverSimParamsAtLevel(0), listOf(), mutableListOf(), UpdateRetrievalParams()
    )
    serversAtLevel[0] = mutableListOf(rootServer)

    for (currentLevel in 1..hierarchyConfig.deepestLevel) {
        val serversAtCurrentLevel = mutableListOf<Server>()
        val parentServers = serversAtLevel[currentLevel - 1]
        for (parent in parentServers!!) {
            for (i in 1..hierarchyConfig.branchingFactor) {
                val childServer = network.generateServer(
                    hierarchyConfig.serverSimParamsAtLevel(currentLevel), listOf(parent), mutableListOf()
                )
                parent.createLink(
                    hierarchyConfig.serverServerLinkSimParamsAtLevel(currentLevel), childServer, MutableLinkState(true)
                )
                childServer.createLink(
                    hierarchyConfig.serverServerLinkSimParamsAtLevel(currentLevel), parent, MutableLinkState(true)
                )
                serversAtCurrentLevel.add(childServer)
            }
        }
        serversAtLevel[currentLevel] = serversAtCurrentLevel
    }
    return serversAtLevel;
}

fun addEdgesToHierarchy(
    network: Network, serverHierarchy: Map<Int, List<Server>>, edgeGroupConfig: EdgeGroupConfiguration
) {
    for (hierarchyEntry in serverHierarchy) {
        val level = hierarchyEntry.key
        val serversAtLevel = hierarchyEntry.value
        val edgesPerServer = edgeGroupConfig.edgesPerServerAtLevel(level)
        val edgeSimParams = edgeGroupConfig.edgeSimParamsAtLevel(level)
        val linkSimParams = edgeGroupConfig.serverEdgeLinkSimParamsAtLevel(level)
        for (server in serversAtLevel) {
            for (i in 1..edgesPerServer) {
                val edge = network.generateEdge(edgeSimParams, listOf(server),
                    // todo: copy interface, implemented by softwareState, with copy method
                    edgeGroupConfig.runningSoftware.toList().map { SoftwareState(it.type, it.versionNumber, it.size) }
                        .toMutableList())
                server.createLink(linkSimParams(), edge, MutableLinkState(true))
                edge.createLink(linkSimParams(), server, MutableLinkState(true))
            }
        }
    }
}
