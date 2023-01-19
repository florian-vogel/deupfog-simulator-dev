import simulator.Simulator
import test.PaperEvalScenarios
import test.clientServerTestPush
import java.io.File

fun main(args: Array<String>) {
    // todo
    // make path relative
    val oldStats =
        File("C:\\Users\\Florian Vogel\\Documents\\Uni\\Simulator\\deupfog-simulator-dev\\analysis\\stats-out")
    oldStats.deleteRecursively()

    val simulationSetup = PaperEvalScenarios().scenario01()
    val simulator = Simulator(simulationSetup)
    simulator.runSimulation()
}
