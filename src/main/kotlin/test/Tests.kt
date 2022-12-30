package test

import main.Simulator
import network.*
import node.*
import software.SoftwareState
import software.SoftwareUpdate
import software.Software

val simpleTransmissionConfig = TransmissionConfig(1) { size, bandwidth, latency ->
    size / bandwidth + latency * 2
}

fun clientServerTestPush(): Simulator.SimulationParams {
    val software = Software("software")
    val networkConfig = NetworkConfig(1, createPushStrategy(), listOf(software))
    val network = Network(networkConfig)
    val serverNode = network.generateServer(
        NodeSimParams(10),
    )
    val edgeNode = network.generateEdge(
        NodeSimParams(10),
        listOf(serverNode),
        listOf(SoftwareState(software, 0, 0)),
    )
    serverNode.createLink(LinkConfig(1, 0, simpleTransmissionConfig), edgeNode, MutableLinkState(true))
    edgeNode.createLink(LinkConfig(1, 0, simpleTransmissionConfig), serverNode, MutableLinkState(true))

    val update = SoftwareUpdate(software, 1, 1) { oldSize -> oldSize + 1 }
    val updates = listOf(Simulator.InitialUpdateParams(update, 100, listOf(serverNode)))

    return Simulator.SimulationParams(network, updates)
}

/*
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
        UpdateRetrievalParams(updateRequestInterval = 30),
        MutableNodeState(true)
    )
    serverNode.addLink(network.UnidirectionalLink(LinkSimParams(1, 0, null), edgeNode, network.MutableLinkState(true)))
    edgeNode.addLink(network.UnidirectionalLink(LinkSimParams(1, 0, null), serverNode, network.MutableLinkState(true)))


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
    serverNode.addLink(network.UnidirectionalLink(LinkSimParams(1, 0, null), edgeNode, network.MutableLinkState(true)))
    edgeNode.addLink(network.UnidirectionalLink(LinkSimParams(1, 0, null), serverNode, network.MutableLinkState(true)))


    val update = SoftwareUpdate(software, 1, 1) { oldSize -> oldSize + 1 }

    val edges = listOf(edgeNode)
    val servers = listOf(serverNode)
    val updates = listOf(Simulator.InitialUpdateParams(update, 100, serverNode))

    return Simulator.SimulationParams(edges, servers, updates)
}
 */