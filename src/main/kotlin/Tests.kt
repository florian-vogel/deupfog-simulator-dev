import Software.SoftwareState
import Software.SoftwareUpdate
import Software.Software

fun clientServerTestPush(): Simulator.SimulationParams {
    val software = Software("software")
    val serverNode = Server(
        NodeSimParams(10),
        listOf(),
        listOf(),
        UpdateRetrievalParams(),
        MutableServerState(true)
    )
    val edgeNode = Edge(
        NodeSimParams(10),
        listOf(serverNode),
        listOf(SoftwareState(software, 0, 0)),
        UpdateRetrievalParams(true),
        MutableNodeState(true)
    )
    serverNode.addLink(UnidirectionalLink(LinkSimParams(1, 0, null), edgeNode, MutableLinkState(true)))
    edgeNode.addLink(UnidirectionalLink(LinkSimParams(1, 0, null), serverNode, MutableLinkState(true)))


    val update = SoftwareUpdate(software, 1, 1) { oldSize -> oldSize + 1 }

    val edges = listOf(edgeNode)
    val servers = listOf(serverNode)
    val updates = listOf(Simulator.InitialUpdateParams(update, 100, serverNode))

    return Simulator.SimulationParams(edges, servers, updates)
}

fun clientServerTestPull(): Simulator.SimulationParams {
    val software = Software("software")
    val serverNode = Server(
        NodeSimParams(10),
        listOf(),
        listOf(),
        UpdateRetrievalParams(),
        MutableServerState(true)
    )
    val edgeNode = Edge(
        NodeSimParams(10),
        listOf(serverNode),
        listOf(SoftwareState(software, 0, 0)),
        UpdateRetrievalParams(sendUpdateRequestsInterval = 30),
        MutableNodeState(true)
    )
    serverNode.addLink(UnidirectionalLink(LinkSimParams(1, 0, null), edgeNode, MutableLinkState(true)))
    edgeNode.addLink(UnidirectionalLink(LinkSimParams(1, 0, null), serverNode, MutableLinkState(true)))


    val update = SoftwareUpdate(software, 1, 1) { oldSize -> oldSize + 1 }

    val edges = listOf(edgeNode)
    val servers = listOf(serverNode)
    val updates = listOf(Simulator.InitialUpdateParams(update, 100, serverNode))

    return Simulator.SimulationParams(edges, servers, updates)
}

fun clientServerTestPushStartOffline(): Simulator.SimulationParams {
    val software = Software("software")
    val serverNode = Server(
        NodeSimParams(10),
        listOf(),
        listOf(),
        UpdateRetrievalParams(),
        MutableServerState(true)
    )
    val edgeNode = Edge(
        NodeSimParams(10, { _, _ -> Simulator.getCurrentTimestamp() + 70 }),
        listOf(serverNode),
        listOf(SoftwareState(software, 0, 0)),
        UpdateRetrievalParams(registerAtServerForUpdates = true),
        MutableNodeState(false)
    )
    serverNode.addLink(UnidirectionalLink(LinkSimParams(1, 0, null), edgeNode, MutableLinkState(true)))
    edgeNode.addLink(UnidirectionalLink(LinkSimParams(1, 0, null), serverNode, MutableLinkState(true)))


    val update = SoftwareUpdate(software, 1, 1) { oldSize -> oldSize + 1 }

    val edges = listOf(edgeNode)
    val servers = listOf(serverNode)
    val updates = listOf(Simulator.InitialUpdateParams(update, 100, serverNode))

    return Simulator.SimulationParams(edges, servers, updates)
}


/*
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
        listOf(Software.SoftwareState(software, 0, 0)),
        UpdateRetrievalParams(),
        MutableServerState(false, null, null)
    )
    val edgeNode = Edge(
        NodeSimParams(10),
        listOf(serverNode),
        listOf(Software.SoftwareState(software, 0, 0)),
        UpdateRetrievalParams(true),
        MutableNodeState(false)
    )
    UnidirectionalLink(serverNode, edgeNode, LinkSimParams(0, 0, ::dummyNextOnlineStateChange), MutableLinkState(false))
    UnidirectionalLink(edgeNode, serverNode, LinkSimParams(0, 0, ::dummyNextOnlineStateChange), MutableLinkState(false))


    val update = Software.SoftwareUpdate(software, 1, 1) { oldSize -> oldSize + 1 }

    val edges = listOf(edgeNode)
    val servers = listOf(serverNode)
    val updates = listOf(Simulator.InitialUpdateParams(update, 0, serverNode))

    return Simulator.SimulationParams(edges, servers, updates)
}


fun createSimpleTest4(): Simulator.SimulationParams {
    val software = Software("software")
    val serverNode = Server(
        NodeSimParams(10), listOf(), listOf(), UpdateRetrievalParams(), MutableServerState(false, null, null)
    )
    val serverNode2 = Server(
        NodeSimParams(10),
        listOf(serverNode),
        listOf(),
        UpdateRetrievalParams(true),
        MutableServerState(false, null, null)
    )
    val edgeNode = Edge(
        NodeSimParams(10) { current, online -> current + 30 },
        listOf(serverNode2),
        listOf(Software.SoftwareState(software, 0, 0)),
        UpdateRetrievalParams(true),
        MutableNodeState(false)
    )
    val edgeNode2 = Edge(
        NodeSimParams(10) { current, online -> current + 30 },
        listOf(serverNode2),
        listOf(Software.SoftwareState(software, 0, 0)),
        UpdateRetrievalParams(true),
        MutableNodeState(false)
    )
    UnidirectionalLink(serverNode, serverNode2, LinkSimParams(0, 0), MutableLinkState(true))
    UnidirectionalLink(serverNode2, serverNode, LinkSimParams(0, 0), MutableLinkState(true))
    UnidirectionalLink(serverNode2, edgeNode, LinkSimParams(0, 0), MutableLinkState(true))
    UnidirectionalLink(edgeNode, serverNode2, LinkSimParams(0, 0), MutableLinkState(true))
    UnidirectionalLink(serverNode2, edgeNode2, LinkSimParams(0, 0), MutableLinkState(true))
    UnidirectionalLink(edgeNode2, serverNode2, LinkSimParams(0, 0), MutableLinkState(true))


    val update = Software.SoftwareUpdate(software, 1, 1) { oldSize -> oldSize + 1 }
    val update2 = Software.SoftwareUpdate(software, 2, 1) { oldSize -> oldSize + 1 }

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
        listOf(Software.SoftwareState(software, 0, 0)),
        UpdateRetrievalParams(),
        MutableServerState(false, null, null)
    )
    val edgeNode = Edge(
        NodeSimParams(10),
        listOf(serverNode),
        listOf(Software.SoftwareState(software, 0, 0)),
        UpdateRetrievalParams(false, 60),
        MutableNodeState(false)
    )
    UnidirectionalLink(
        serverNode, edgeNode, LinkSimParams(0, 0) { _, online -> if (!online) 100 else null }, MutableLinkState(false)
    )
    UnidirectionalLink(
        edgeNode, serverNode, LinkSimParams(0, 0) { _, online -> if (!online) 100 else null }, MutableLinkState(false)
    )


    val update = Software.SoftwareUpdate(software, 1, 1) { oldSize -> oldSize + 1 }
    val update2 = Software.SoftwareUpdate(software, 2, 1) { oldSize -> oldSize + 1 }

    val edges = listOf(edgeNode)
    val servers = listOf(serverNode)
    val updates = listOf(
        Simulator.InitialUpdateParams(update, 0, serverNode), Simulator.InitialUpdateParams(update2, 20, serverNode)
    )

    return Simulator.SimulationParams(edges, servers, updates)
}
*/