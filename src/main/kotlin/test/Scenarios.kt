package test

import network.LinkConfig
import network.*
import node.*
import simulator.InitialUpdateParams
import simulator.SimulationSetup
import software.Software
import software.SoftwareState
import software.SoftwareUpdate

const val UPDATE_INIT_TIMESTAMP = 500
const val UPDATE_INIT_TIMESTAMP_02 = 1500

val simpleTransmission = TransmissionConfig(
    1
) { size, bandwidth, delay -> size / bandwidth + delay * 2 }

val simplePackageConfig = PackagesConfigServer(
    100, 100, 100
) { _, numOfPackages -> numOfPackages }

class Scenarios {
    fun testScenario(): SimulationSetup {
        val software = Software("testSoftware")
        val update01 = SoftwareUpdate(software, 1, 100000) { 0 }
        val deepestLevel = 2
        val update02 = SoftwareUpdate(software, 2, 100000) { 0 }

        val networkConfig = NetworkConfig(
            createPushStrategy(),
            listOf(software),
            simplePackageConfig
        )
        val hierarchyConfig = HierarchyConfiguration(
            deepestLevel,
            1,
            { NodeConfig(5000000) },
            { LinkConfig(10000, 10, simpleTransmission) },
            listOf(update01)
        )
        val edgeGroupConfigs = listOf(EdgeGroupConfiguration(
            listOf(SoftwareState(software, 0, 0)),
            createPushStrategy(),
            { NodeConfig(1000000) },
            { LinkConfig(10000, 10, simpleTransmission) },
            { level -> if (level == deepestLevel) 2 else 0 }
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

    fun testScenario02(): SimulationSetup {
        val software = Software("testSoftware")
        val update01 = SoftwareUpdate(software, 1, 100000) { 0 }
        val deepestLevel = 2
        val update02 = SoftwareUpdate(software, 2, 100000) { 0 }

        val networkConfig = NetworkConfig(
            createPullStrategy(20000),
            listOf(software),
            simplePackageConfig
        )
        val hierarchyConfig = HierarchyConfiguration(
            deepestLevel,
            2,
            { NodeConfig(5000000) },
            { LinkConfig(10000, 10, simpleTransmission) },
            listOf(update01)
        )
        val edgeGroupConfigs = listOf(EdgeGroupConfiguration(
            listOf(SoftwareState(software, 0, 0)),
            createPullStrategy(20000),
            { NodeConfig(1000000) },
            { LinkConfig(1000, 1, simpleTransmission) },
            { level -> if (level == deepestLevel) 10 else 0 }
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
