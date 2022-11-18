fun main(args: Array<String>) {
    val software = Software("software")
    val serverNode = Server(mutableListOf(), 10, listOf(), listOf(software), UpdateRetrievalParams())
    val edgeNode = Edge(mutableListOf(), 10, listOf(serverNode), listOf(software), UpdateRetrievalParams(true))
    val link = UnidirectionalLink(serverNode, edgeNode)
    serverNode.addLink(link)

    val metrics = MetricsCollector("", listOf(edgeNode), listOf(serverNode))
    val simulator = Simulator(metrics)
    simulator.setMetrics(metrics)

    val update = SoftwareUpdate(software, 2, 1)
    val p = UpdateResponse(serverNode, serverNode, 1, update)

    val initialTimedCallbacks = listOf(InitPackageAtNodeCallback(0, p))

    simulator.runSimulation(initialTimedCallbacks)
    metrics.printMetrics()
}
