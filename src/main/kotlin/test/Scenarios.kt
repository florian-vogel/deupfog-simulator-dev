package test

import network.*
import node.*
import simulator.InitialUpdateParams
import simulator.SimulationSetup
import software.Software
import software.SoftwareState
import software.SoftwareUpdate

const val UPDATE_INIT_TIMESTAMP = 200
const val UPDATE_INIT_TIMESTAMP_02 = 2000

val simpleTransmission = TransmissionConfig(
   100
) { size, bandwidth, delay -> size / bandwidth + delay * 2 }

val simplePackageConfig = PackagesConfigServer(
    10, 10, 10
) { _, numOfPackages -> numOfPackages * 10 + 10 }

val randomOfflineBehaviour = { maxRandom: Int -> { current: Int, online: Boolean -> current + (1..maxRandom).random() }}

class Scenarios {
    fun testScenario(): SimulationSetup {
        val software = Software("testSoftware")
        val update01 = SoftwareUpdate(software, 1, 5000000) { 0 }
        val deepestLevel = 2
        val update02 = SoftwareUpdate(software, 2, 5000000) { 0 }

        val networkConfig = NetworkConfig(
            createPushStrategy(),
            listOf(software),
            simplePackageConfig
        )
        val hierarchyConfig = HierarchyConfiguration(
            deepestLevel,
            2,
            { NodeConfig(100000000) },
            { LinkConfig(10000, 10, simpleTransmission) },
        )
        val edgeGroupConfigs = listOf(EdgeGroupConfiguration(
            listOf(SoftwareState(software, 0, 0)),
            createPushStrategy(),
            { NodeConfig(100000000, randomOfflineBehaviour(3000)) },
            { LinkConfig(10000, 10, simpleTransmission) },
            { level -> if (level == deepestLevel) 20 else 0 }
        ))
        val network = generateHierarchicalNetwork(
            networkConfig,
            hierarchyConfig,
            edgeGroupConfigs
        )
        val updateParams =
            listOf<InitialUpdateParams>(
                InitialUpdateParams(update01, UPDATE_INIT_TIMESTAMP, network.updateInitializationServers),
                InitialUpdateParams(update02, UPDATE_INIT_TIMESTAMP_02, network.updateInitializationServers)
            )
        return SimulationSetup(network, updateParams)
    }

    fun testScenario2(): SimulationSetup {
        val software = Software("testSoftware")
        val update01 = SoftwareUpdate(software, 1, 100000) { 0 }
        val deepestLevel = 1
        val update02 = SoftwareUpdate(software, 2, 100000) { 0 }

        val networkConfig = NetworkConfig(
            createPullStrategy(2000),
            listOf(software),
            simplePackageConfig
        )
        val hierarchyConfig = HierarchyConfiguration(
            deepestLevel,
            1,
            { NodeConfig(5000000) },
            { LinkConfig(10000, 10, simpleTransmission) },
        )
        val edgeGroupConfigs = listOf(EdgeGroupConfiguration(
            listOf(SoftwareState(software, 0, 0)),
            createPullStrategy(1000),
            { NodeConfig(1000000) },
            { LinkConfig(10000, 10, simpleTransmission) },
            { level -> if (level == deepestLevel) 1 else 0 }
        ))
        val network = generateHierarchicalNetwork(
            networkConfig,
            hierarchyConfig,
            edgeGroupConfigs
        )
        val updateParams =
            listOf(
                InitialUpdateParams(update01, UPDATE_INIT_TIMESTAMP, network.updateInitializationServers),
                InitialUpdateParams(update02, UPDATE_INIT_TIMESTAMP_02, network.updateInitializationServers)
            )
        return SimulationSetup(network, updateParams)
    }

}
