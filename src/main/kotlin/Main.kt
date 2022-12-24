import java.io.File

fun main(args: Array<String>) {
    val oldStats =
        File("C:\\Users\\Florian Vogel\\Documents\\Uni\\Simulator\\deupfog-simulator-dev\\analysis\\stats-out")
    oldStats.deleteRecursively()

    val simulator = Simulator()
    val runSimulationParams = Scenarios().scenarioWithTwoUpdates()
    //val runSimulationParams = createSimpleTest4()
    val simConfigParams = Simulator.SimConfigParams(true, 20000)
    simulator.runSimulation(runSimulationParams, simConfigParams)
}
