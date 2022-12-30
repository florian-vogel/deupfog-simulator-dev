import main.Simulator
import test.Scenarios
import java.io.File

fun main(args: Array<String>) {
    val oldStats =
        File("C:\\Users\\Florian Vogel\\Documents\\Uni\\Simulator\\deupfog-simulator-dev\\analysis\\stats-out")
    oldStats.deleteRecursively()

    val simulationParams = Scenarios().testScenario()
    val simConfigParams = Simulator.SimConfigParams(20000)
    val simulator = Simulator(simulationParams, simConfigParams)
    simulator.runSimulation()
}
