package test

import network.LinkConfig
import main.Simulator
import network.*
import node.*
import software.Software
import software.SoftwareState
import software.SoftwareUpdate

const val UPDATE_INIT_TIMESTAMP = 100
const val UPDATE_INIT_TIMESTAMP_02 = 150

val simpleTransmission = TransmissionConfig(
    1
) { size, bandwidth, delay -> size / bandwidth + delay * 2 }

class Scenarios {
    fun testScenario(): Simulator.SimulationParams {
        // 1 size unit = 1 byte
        // 1 temp unit = 1 ms
        val software = Software("testSoftware")
        val update01 = SoftwareUpdate(software, 1, 1) { 1 }
        val deepestLevel = 1
        // val update02 = Software.SoftwareUpdate(software, 2, 1) { 1 }

        val networkConfig = NetworkConfig(
            1,
            createPushStrategy(),
            listOf(software)
        )
        val hierarchyConfig = HierarchyConfiguration(
            deepestLevel,
            1,
            { NodeSimParams(10) },
            { LinkConfig(1, 1, simpleTransmission) },
            listOf(update01)
        )
        val edgeGroupConfigs = listOf(EdgeGroupConfiguration(
            listOf(SoftwareState(software, 0, 0)),
            createPushStrategy(),
            { NodeSimParams(10) },
            { LinkConfig(1, 1, simpleTransmission) },
            { level -> if (level == deepestLevel) 1 else 0 }
        ))
        val network = generateHierarchicalNetwork(
            networkConfig,
            hierarchyConfig,
            edgeGroupConfigs
        )
        val updateParams =
            listOf(Simulator.InitialUpdateParams(update01, UPDATE_INIT_TIMESTAMP, network.updateInitializationServers))
        return Simulator.SimulationParams(network, updateParams)
    }

    fun testScenario02(): Simulator.SimulationParams {
        // 1 size unit = 1 byte
        // 1 temp unit = 1 ms
        val software = Software("testSoftware")
        val update01 = SoftwareUpdate(software, 1, 1) { 1 }
        val deepestLevel = 1
        val update02 = SoftwareUpdate(software, 2, 1) { 1 }

        val networkConfig = NetworkConfig(
            1,
            createPullStrategy(70),
            listOf(software)
        )
        val hierarchyConfig = HierarchyConfiguration(
            deepestLevel,
            1,
            { NodeSimParams(10) },
            { LinkConfig(1, 1, simpleTransmission) },
            listOf(update01)
        )
        val edgeGroupConfigs = listOf(EdgeGroupConfiguration(
            listOf(SoftwareState(software, 0, 0)),
            createPullStrategy(70),
            { NodeSimParams(10) },
            { LinkConfig(1, 1, simpleTransmission) },
            { level -> if (level == deepestLevel) 1 else 0 }
        ))
        val network = generateHierarchicalNetwork(
            networkConfig,
            hierarchyConfig,
            edgeGroupConfigs
        )
        val updateParams =
            listOf(
                Simulator.InitialUpdateParams(update01, UPDATE_INIT_TIMESTAMP, network.updateInitializationServers),
                Simulator.InitialUpdateParams(update02, UPDATE_INIT_TIMESTAMP_02, network.updateInitializationServers)
            )
        return Simulator.SimulationParams(network, updateParams)
    }
}
