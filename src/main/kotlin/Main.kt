fun main(args: Array<String>) {
    val simulator = Simulator()
    val runSimulationParams = createSimpleTestPush()
    simulator.runSimulation(runSimulationParams)
}
