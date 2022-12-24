const val UPDATE_INIT_TIMESTAMP = 5000

class Scenarios {
    fun testScenario(): Simulator.SimulationParams {
        // 1 size unit = 1 byte
        // 1 temp unit = 1 ms
        val software = Software("testSoftware")
        val update01 = SoftwareUpdate(software, 1, 1) { 1 }
        // val update02 = SoftwareUpdate(software, 2, 1) { 1 }
        val edgeGroup = EdgeGroupConfiguration(
            listOf(SoftwareState(software, 0, 0)),
            // UpdateRetrievalParams(registerAtServerForUpdates = false, sendUpdateRequestsInterval = 20),
            UpdateRetrievalParams(registerAtServerForUpdates = true),
            { NodeSimParams(100) },
            { LinkSimParams(1, 1) },
        ) { level ->
            if (level == 1) {
                1
            } else {
                1
            }
        }

        val scenario01Configuration = ScenarioConfiguration(
            1,
            1,
            ({ NodeSimParams(100, null, null) }),
            ({ LinkSimParams(1, 1) }),
            listOf(update01),
            listOf(edgeGroup)
        )
        return generateScenario(scenario01Configuration)
    }

    fun scenarioWithTwoUpdates(): Simulator.SimulationParams {
        // 1 size unit = 1 byte
        // 1 temp unit = 1 ms
        val software = Software("testSoftware")
        val update01 = SoftwareUpdate(software, 1, 100000) { 100 }
        val update02 = SoftwareUpdate(software, 2, 100000) { 100 }
        val edgeGroup = EdgeGroupConfiguration(
            listOf(SoftwareState(software, 0, 0)),
            UpdateRetrievalParams(registerAtServerForUpdates = true),
            //UpdateRetrievalParams(registerAtServerForUpdates = false, sendUpdateRequestsInterval = 3000),
            { NodeSimParams(1000000) },
            { LinkSimParams(1000, 20) },
        ) { level ->
            if (level == 6) {
                3
            } else if (level == 5) {
                0
            } else {
                0
            }
        }

        val scenario01Configuration = ScenarioConfiguration(
            6,
            2,
            ({ NodeSimParams(5000000) }),
            ({ LinkSimParams(1000, 20) }),
            listOf(update01, update02),
            listOf(edgeGroup)
        )
        return generateScenario(scenario01Configuration)
    }

    data class ScenarioConfiguration(
        val hierarchyLevel: Int,
        val serverDegree: Int,
        val serverSimParamsAtLevel: (level: Int) -> NodeSimParams,
        val interServerLinkSimParamsAtLevel: (level: Int) -> LinkSimParams,
        val updates: List<SoftwareUpdate>,
        val edgeGroups: List<EdgeGroupConfiguration>
    )

    data class EdgeGroupConfiguration(
        val runningSoftware: List<SoftwareState>,
        val updateRetrievalParams: UpdateRetrievalParams,
        val edgeSimParamsAtLevel: (level: Int) -> NodeSimParams,
        val serverToEdgeLinkSimParamsAtLevel: (level: Int) -> LinkSimParams,
        val edgesPerServerAtLevel: (level: Int) -> Int,
    )

    fun generateScenario(configuration: ScenarioConfiguration): Simulator.SimulationParams {
        val serverHierarchy = generateServerHierarchy(
            configuration.hierarchyLevel,
            configuration.serverDegree,
            configuration.serverSimParamsAtLevel,
            configuration.interServerLinkSimParamsAtLevel
        )
        val servers = serverHierarchy.values.flatten()

        val edges = mutableListOf<Edge>()
        for (edgeGroupConfiguration in configuration.edgeGroups) {
            edges += addEdgesToHierarchy(
                serverHierarchy,
                edgeGroupConfiguration.runningSoftware,
                edgeGroupConfiguration.updateRetrievalParams,
                edgeGroupConfiguration.edgesPerServerAtLevel,
                edgeGroupConfiguration.edgeSimParamsAtLevel,
                edgeGroupConfiguration.serverToEdgeLinkSimParamsAtLevel,
            )
        }

        val initialUpdateParams = configuration.updates.map {
            Simulator.InitialUpdateParams(
                it, UPDATE_INIT_TIMESTAMP, serverHierarchy[0]!![0] as Server
            )
        }

        return Simulator.SimulationParams(
            edges, servers, initialUpdateParams
        )
    }
}

// todo: ServerHierarchy class, this method in constructor
private fun generateServerHierarchy(
    levels: Int,
    serverDegree: Int,
    serverSimParamsAtLevel: (level: Int) -> NodeSimParams,
    linkSimParamsAtLevel: (level: Int) -> LinkSimParams
): Map<Int, List<Server>> {
    val serversAtLevel = mutableMapOf<Int, List<Server>>()
    val rootServer = Server(
        serverSimParamsAtLevel(0), listOf(), listOf(), UpdateRetrievalParams(), MutableServerState(true)
    )
    serversAtLevel[0] = mutableListOf(rootServer)

    for (currentLevel in 1..levels) {
        val serversAtCurrentLevel = mutableListOf<Server>()
        val parentServers = serversAtLevel[currentLevel - 1]
        for (parent in parentServers!!) {
            for (i in 1..serverDegree) {
                val childServer = Server(
                    serverSimParamsAtLevel(currentLevel),
                    listOf(parent),
                    listOf(),
                    UpdateRetrievalParams(registerAtServerForUpdates = true),
                    MutableServerState(true)
                )
                // TODO: replace with bidirectional
                UnidirectionalLink(
                    linkSimParamsAtLevel(currentLevel), parent, childServer, MutableLinkState(true)
                )
                UnidirectionalLink(
                    linkSimParamsAtLevel(currentLevel), childServer, parent, MutableLinkState(true)
                )
                serversAtCurrentLevel.add(childServer)
            }
        }
        serversAtLevel[currentLevel] = serversAtCurrentLevel
    }
    return serversAtLevel;
}

fun addEdgesToHierarchy(
    serverHierarchy: Map<Int, List<Server>>,
    initRunningSoftware: List<SoftwareState>,
    updateRetrievalParams: UpdateRetrievalParams,
    edgesPerServerAtLevel: (level: Int) -> Int,
    edgeSimParamsAtLevel: (level: Int) -> NodeSimParams,
    linkSimParamsAtLevel: (level: Int) -> LinkSimParams,
): List<Edge> {
    val edges = mutableListOf<Edge>()
    for (hierarchyEntry in serverHierarchy) {
        val level = hierarchyEntry.key
        val serversAtLevel = hierarchyEntry.value
        val edgesPerServer = edgesPerServerAtLevel(level)
        val edgeSimParams = edgeSimParamsAtLevel(level)
        val linkSimParams = linkSimParamsAtLevel(level)
        for (server in serversAtLevel) {
            for (i in 1..edgesPerServer) {
                val edge = Edge(
                    edgeSimParams,
                    listOf(server),
                    // todo: copy interface, implemented by softwareState, with copy method
                    initRunningSoftware.toList().map { SoftwareState(it.type, it.versionNumber, it.size) },
                    updateRetrievalParams,
                    MutableNodeState(false)
                )
                edges.add(edge)
                UnidirectionalLink(linkSimParams, server, edge, MutableLinkState(true))
                UnidirectionalLink(linkSimParams, edge, server, MutableLinkState(true))
            }
        }
    }
    return edges
}
