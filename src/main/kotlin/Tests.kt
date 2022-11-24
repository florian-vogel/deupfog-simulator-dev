fun createSimpleTestPush(): Simulator.SimulationParams {
    val software = Software("software")
    val serverNode = Server(
        10,
        listOf(),
        listOf(SoftwareState(software, 0, 0)),
        UpdateRetrievalParams(),
        InitialServerState(false, null, null)
    )
    val edgeNode = Edge(
        10,
        listOf(serverNode),
        listOf(SoftwareState(software, 0, 0)),
        UpdateRetrievalParams(true),
        InitialNodeState(false)
    )
    UnidirectionalLink(serverNode, edgeNode)
    UnidirectionalLink(edgeNode, serverNode)


    val update = SoftwareUpdate(software, 1, 1, 0) { oldSize -> oldSize + 1 }

    val edges = listOf(edgeNode)
    val servers = listOf(serverNode)
    val updates = listOf(Simulator.InitialUpdateParams(update, 0, serverNode))

    return Simulator.SimulationParams(edges, servers, updates)
}

fun createSimpleTest2(): Simulator.SimulationParams {
    val software = Software("software")
    // TODO: make list of registerd edges initializable with non empty
    val serverNode = Server(
        10,
        listOf(),
        listOf(SoftwareState(software, 0, 0)),
        UpdateRetrievalParams(),
        InitialServerState(false, null, null)
    )
    val serverNode2 = Server(
        10,
        listOf(serverNode),
        listOf(SoftwareState(software, 0, 0)),
        UpdateRetrievalParams(true),
        InitialServerState(false, null, null)
    )
    val edgeNode = Edge(
        10,
        listOf(serverNode2),
        listOf(SoftwareState(software, 0, 0)),
        UpdateRetrievalParams(true),
        InitialNodeState(false)
    )
    val edgeNode2 = Edge(
        10,
        listOf(serverNode2),
        listOf(SoftwareState(software, 0, 0)),
        UpdateRetrievalParams(true),
        InitialNodeState(false)
    )
    UnidirectionalLink(serverNode, serverNode2)
    UnidirectionalLink(serverNode2, serverNode)
    UnidirectionalLink(serverNode2, edgeNode)
    UnidirectionalLink(edgeNode, serverNode2)
    UnidirectionalLink(serverNode2, edgeNode2)
    UnidirectionalLink(edgeNode2, serverNode2)


    val update = SoftwareUpdate(software, 1, 1, 0) { oldSize -> oldSize + 1 }
    val update2 = SoftwareUpdate(software, 2, 1, 0) { oldSize -> oldSize + 1 }

    val edges = listOf(edgeNode)
    val servers = listOf(serverNode, serverNode2)
    val updates = listOf(
        Simulator.InitialUpdateParams(update, 0, serverNode), Simulator.InitialUpdateParams(update2, 0, serverNode)
    )

    return Simulator.SimulationParams(edges, servers, updates)
}

fun createSimpleTest3(): Simulator.SimulationParams {
    val software = Software("software")
    val serverNode = Server(
        10, listOf(), listOf(), UpdateRetrievalParams(), InitialServerState(false, null, null)
    )
    val edgeNode = Edge(
        10,
        listOf(serverNode),
        listOf(SoftwareState(software, 0, 0)),
        UpdateRetrievalParams(true),
        InitialNodeState(false)
    )
    val edgeNode2 = Edge(
        10,
        listOf(serverNode),
        listOf(SoftwareState(software, 0, 0)),
        UpdateRetrievalParams(true),
        InitialNodeState(false)
    )
    UnidirectionalLink(serverNode, edgeNode)
    UnidirectionalLink(edgeNode, serverNode)
    UnidirectionalLink(serverNode, edgeNode2)
    UnidirectionalLink(edgeNode2, serverNode)


    val update = SoftwareUpdate(software, 1, 1, 100) { oldSize -> oldSize + 1 }
    val update2 = SoftwareUpdate(software, 2, 1, 200) { oldSize -> oldSize + 1 }

    val edges = listOf(edgeNode, edgeNode2)
    val servers = listOf(serverNode)
    val updates = listOf(
        Simulator.InitialUpdateParams(update, 100, serverNode), Simulator.InitialUpdateParams(update2, 200, serverNode)
    )

    return Simulator.SimulationParams(edges, servers, updates)
}

fun createSimpleTest4(): Simulator.SimulationParams {
    val software = Software("software")
    val serverNode = Server(
        10, listOf(), listOf(), UpdateRetrievalParams(), InitialServerState(false, null, null)
    )
    val serverNode2 = Server(
        10, listOf(serverNode), listOf(), UpdateRetrievalParams(true), InitialServerState(false, null, null)
    )
    val edgeNode = Edge(
        10,
        listOf(serverNode2),
        listOf(SoftwareState(software, 0, 0)),
        UpdateRetrievalParams(true),
        InitialNodeState(false)
    )
    val edgeNode2 = Edge(
        10,
        listOf(serverNode2),
        listOf(SoftwareState(software, 0, 0)),
        UpdateRetrievalParams(true),
        InitialNodeState(false)
    )
    UnidirectionalLink(serverNode, serverNode2)
    UnidirectionalLink(serverNode2, serverNode)
    UnidirectionalLink(serverNode2, edgeNode)
    UnidirectionalLink(edgeNode, serverNode2)
    UnidirectionalLink(serverNode2, edgeNode2)
    UnidirectionalLink(edgeNode2, serverNode2)


    val update = SoftwareUpdate(software, 1, 1, 100) { oldSize -> oldSize + 1 }
    val update2 = SoftwareUpdate(software, 2, 1, 200) { oldSize -> oldSize + 1 }

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
        10,
        listOf(),
        listOf(SoftwareState(software, 0, 0)),
        UpdateRetrievalParams(),
        InitialServerState(false, null, null)
    )
    val edgeNode = Edge(
        10,
        listOf(serverNode),
        listOf(SoftwareState(software, 0, 0)),
        UpdateRetrievalParams(false, 60),
        InitialNodeState(false)
    )
    UnidirectionalLink(serverNode, edgeNode)
    UnidirectionalLink(edgeNode, serverNode)


    val update = SoftwareUpdate(software, 1, 1, 0) { oldSize -> oldSize + 1 }
    val update2 = SoftwareUpdate(software, 2, 1, 20) { oldSize -> oldSize + 1 }

    val edges = listOf(edgeNode)
    val servers = listOf(serverNode)
    val updates = listOf(Simulator.InitialUpdateParams(update, 0, serverNode), Simulator.InitialUpdateParams(update2, 20, serverNode))

    return Simulator.SimulationParams(edges, servers, updates)
}
