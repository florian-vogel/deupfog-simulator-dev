fun dummyNextOnlineStateChange(timestamp: Int, online: Boolean): Int? {
    return if (!online) 100 else null
}

fun dummyNextOnlineStateChange2(timestamp: Int, online: Boolean): Int? {
    return timestamp + 30
}

fun createSimpleTestPush(): Simulator.SimulationParams {
    val software = Software("software")
    val serverNode = Server(
        NodeSimParams(10),
        listOf(),
        listOf(SoftwareState(software, 0, 0)),
        UpdateRetrievalParams(),
        InitialServerState(false, null, null)
    )
    val edgeNode = Edge(
        NodeSimParams(10),
        listOf(serverNode),
        listOf(SoftwareState(software, 0, 0)),
        UpdateRetrievalParams(true),
        MutableNodeState(false)
    )
    UnidirectionalLink(serverNode, edgeNode, LinkSimParams(0, 0, ::dummyNextOnlineStateChange), MutableLinkState(false))
    UnidirectionalLink(edgeNode, serverNode, LinkSimParams(0, 0, ::dummyNextOnlineStateChange), MutableLinkState(false))


    val update = SoftwareUpdate(software, 1, 1) { oldSize -> oldSize + 1 }

    val edges = listOf(edgeNode)
    val servers = listOf(serverNode)
    val updates = listOf(Simulator.InitialUpdateParams(update, 0, serverNode))

    return Simulator.SimulationParams(edges, servers, updates)
}


fun createSimpleTest4(): Simulator.SimulationParams {
    val software = Software("software")
    val serverNode = Server(
        NodeSimParams(10), listOf(), listOf(), UpdateRetrievalParams(), InitialServerState(false, null, null)
    )
    val serverNode2 = Server(
        NodeSimParams(10),
        listOf(serverNode),
        listOf(),
        UpdateRetrievalParams(true),
        InitialServerState(false, null, null)
    )
    val edgeNode = Edge(
        NodeSimParams(10) { current, online -> current + 30 },
        listOf(serverNode2),
        listOf(SoftwareState(software, 0, 0)),
        UpdateRetrievalParams(true),
        MutableNodeState(false)
    )
    val edgeNode2 = Edge(
        NodeSimParams(10) { current, online -> current + 30 },
        listOf(serverNode2),
        listOf(SoftwareState(software, 0, 0)),
        UpdateRetrievalParams(true),
        MutableNodeState(false)
    )
    UnidirectionalLink(serverNode, serverNode2, LinkSimParams(0, 0), MutableLinkState(true))
    UnidirectionalLink(serverNode2, serverNode, LinkSimParams(0, 0), MutableLinkState(true))
    UnidirectionalLink(serverNode2, edgeNode, LinkSimParams(0, 0), MutableLinkState(true))
    UnidirectionalLink(edgeNode, serverNode2, LinkSimParams(0, 0), MutableLinkState(true))
    UnidirectionalLink(serverNode2, edgeNode2, LinkSimParams(0, 0), MutableLinkState(true))
    UnidirectionalLink(edgeNode2, serverNode2, LinkSimParams(0, 0), MutableLinkState(true))


    val update = SoftwareUpdate(software, 1, 1) { oldSize -> oldSize + 1 }
    val update2 = SoftwareUpdate(software, 2, 1) { oldSize -> oldSize + 1 }

    val edges = listOf(edgeNode, edgeNode2)
    val servers = listOf(serverNode, serverNode2)
    val updates = listOf(
        Simulator.InitialUpdateParams(update, 100, serverNode), Simulator.InitialUpdateParams(update2, 200, serverNode)
    )

    return Simulator.SimulationParams(edges, servers, updates)
}


fun createSimpleTestPull(): Simulator.SimulationParams {
    val software = Software("software")
    val serverNode = Server(
        NodeSimParams(10),
        listOf(),
        listOf(SoftwareState(software, 0, 0)),
        UpdateRetrievalParams(),
        InitialServerState(false, null, null)
    )
    val edgeNode = Edge(
        NodeSimParams(10),
        listOf(serverNode),
        listOf(SoftwareState(software, 0, 0)),
        UpdateRetrievalParams(false, 60),
        MutableNodeState(false)
    )
    UnidirectionalLink(
        serverNode, edgeNode, LinkSimParams(0, 0) { _, online -> if (!online) 100 else null }, MutableLinkState(false)
    )
    UnidirectionalLink(
        edgeNode, serverNode, LinkSimParams(0, 0) { _, online -> if (!online) 100 else null }, MutableLinkState(false)
    )


    val update = SoftwareUpdate(software, 1, 1) { oldSize -> oldSize + 1 }
    val update2 = SoftwareUpdate(software, 2, 1) { oldSize -> oldSize + 1 }

    val edges = listOf(edgeNode)
    val servers = listOf(serverNode)
    val updates = listOf(
        Simulator.InitialUpdateParams(update, 0, serverNode), Simulator.InitialUpdateParams(update2, 20, serverNode)
    )

    return Simulator.SimulationParams(edges, servers, updates)
}

fun testScenario01(): Simulator.SimulationParams {
    val software = Software("software")
    val hierarchyLevel = 4
    val serverDegree = 2
    val edgesPerServer = 10
    val linkBandwidth = 10
    val latency = 10
    val nodeCapacity = 100
    val updateSize = 10
    val updateInitializationInstant = 200

    val serversToLevel = generateServerHierarchy(hierarchyLevel, serverDegree) { LinkSimParams(linkBandwidth, latency) }
    val serversInLowestHierarchy = serversToLevel.filter { it.hierarchyLevel == hierarchyLevel }.map { it.server }
    val rootServer = serversToLevel.find { it.hierarchyLevel == 0 }!!.server
    val servers = serversToLevel.map { it.server }

    val edgeSoftwareState = SoftwareState(software, 0, 1)
    val edges = addEdgesToServers(
        serversInLowestHierarchy,
        edgesPerServer,
        NodeSimParams(nodeCapacity),
        listOf(edgeSoftwareState),
        UpdateRetrievalParams(true),
        LinkSimParams(linkBandwidth, latency)
    )
    val update = SoftwareUpdate(software, 1, updateSize) { 1 }
    return Simulator.SimulationParams(
        edges,
        servers,
        listOf(Simulator.InitialUpdateParams(update, updateInitializationInstant, rootServer as UpdateReceiverNode))
    )
}

data class ServerToLevel(val server: Server, val hierarchyLevel: Int)

fun generateServerHierarchy(
    levels: Int,
    serverDegree: Int,
    simParamsAtLevel: (level: Int) -> LinkSimParams
): List<ServerToLevel> {
    var currentLevel = 0
    val serversToLevel = mutableListOf<ServerToLevel>()
    val rootServer = Server(
        NodeSimParams(10),
        listOf(),
        listOf(),
        UpdateRetrievalParams(registerAtServerForUpdates = true),
        InitialServerState(true, null, null)
    )
    serversToLevel.add(
        ServerToLevel(rootServer, 0)
    )
    while (currentLevel < levels) {
        currentLevel++;
        val parentServers = serversToLevel.filter { it.hierarchyLevel == currentLevel - 1 }
        for (parent in parentServers) {
            for (i in 1..serverDegree) {
                val childServer = Server(
                    NodeSimParams(10),
                    listOf(parent.server),
                    listOf(),
                    UpdateRetrievalParams(registerAtServerForUpdates = true),
                    InitialServerState(true, null, null)
                )
                UnidirectionalLink(parent.server, childServer, simParamsAtLevel(currentLevel), MutableLinkState(true))
                UnidirectionalLink(childServer, parent.server, simParamsAtLevel(currentLevel), MutableLinkState(true))
                serversToLevel.add(ServerToLevel(childServer, currentLevel))
            }
        }

    }
    return serversToLevel;
}

fun addEdgesToServers(
    servers: List<Server>,
    edgesPerServer: Int,
    nodeSimParams: NodeSimParams,
    runningSoftware: List<SoftwareState>,
    updateRetrievalParams: UpdateRetrievalParams,
    linkSimParams: LinkSimParams,
): List<Edge> {
    val addedEdges = mutableListOf<Edge>()
    for (server in servers) {
        for (i in 1..edgesPerServer) {
            val edge =
                Edge(nodeSimParams, listOf(server), runningSoftware, updateRetrievalParams, MutableNodeState(false))
            addedEdges.add(edge)
            UnidirectionalLink(server, edge, linkSimParams, MutableLinkState(true))
            UnidirectionalLink(edge, server, linkSimParams, MutableLinkState(true))
        }
    }
    return addedEdges
}