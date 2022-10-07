fun main(args: Array<String>) {
    val simpleTestParams = createSimpleTest()
    val network = simpleTestParams.first
    val updateScheduler = simpleTestParams.second

    val simulator = Simulator(updateScheduler)
    Simulator.setNetwork(network)
    simulator.runSimulation()
}
