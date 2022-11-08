fun main(args: Array<String>) {
    val simpleTestParams = createSimpleTestWithSpecificSoftwareVersions()

    val initialCallbacks = simpleTestParams

    val simulator = Simulator()
    simulator.runSimulation(initialCallbacks)
    Simulator.metrics.printMetrics()
}
