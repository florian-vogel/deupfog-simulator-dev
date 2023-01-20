import simulator.Simulator
import simulator.SimulatorConfig
import test.PaperEvalScenarios
import java.io.File

val defaultSimulatorConfig = SimulatorConfig(
    "test",
    60000
)

fun main(args: Array<String>) {
    // todo
    // make path relative
    val oldStats =
        File("C:\\Users\\Florian Vogel\\Documents\\Uni\\Simulator\\deupfog-simulator-dev\\metrics\\csv_data")
    oldStats.deleteRecursively()


    //val pullUnreliableSimSetup = PaperEvalScenarios().scenarioPullReference()
    //val pullUnreliableSim = Simulator(pullUnreliableSimSetup, SimulatorConfig("pullReference", 60000))
    //pullUnreliableSim.runSimulation()
    PaperEvalScenarios().runAll()
    //val pushRefSimSetup = PaperEvalScenarios().scenarioPushReference()
    //val pushRefSim = Simulator(pushRefSimSetup, SimulatorConfig("pushReference", 60000))
    //pushRefSim.runSimulation()
}


