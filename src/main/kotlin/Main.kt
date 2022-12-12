fun main(args: Array<String>) {
    val simulator = Simulator()
    val runSimulationParams = testScenario01()
    //val runSimulationParams = createSimpleTest4()
    val simConfigParams = Simulator.SimConfigParams(true)
    simulator.runSimulation(runSimulationParams, simConfigParams)
}
