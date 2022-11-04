fun main(args: Array<String>) {
    val simpleTestParams = createSimpleTestPull()
    val network = simpleTestParams.first
    val initialCallbacks = simpleTestParams.second

    val simulator = Simulator()
    simulator.runSimulation(network, initialCallbacks)
}
