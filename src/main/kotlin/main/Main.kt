import main.Scenarios
import main.Simulator
import java.io.File

fun main(args: Array<String>) {
    val oldStats =
        File("C:\\Users\\Florian Vogel\\Documents\\Uni\\Simulator\\deupfog-simulator-dev\\analysis\\stats-out")
    oldStats.deleteRecursively()

    //val runSimulationParams = Scenarios().testScenario()
    val runSimulationParams = Scenarios().testScenario()
    val simConfigParams = Simulator.SimConfigParams(20000)
    val simulator = Simulator(runSimulationParams, simConfigParams)
    simulator.runSimulation()
}
