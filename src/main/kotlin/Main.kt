fun main(args: Array<String>) {
    val simulator = Simulator()
    val runSimulationParams = createSimpleTestPull()
    simulator.runSimulation(runSimulationParams)
}
