package main

import LinkSimParams
import network.*
import node.*
import software.Software
import software.SoftwareState
import software.SoftwareUpdate

const val UPDATE_INIT_TIMESTAMP = 100

// todo:
// allow configuration of global simulation parameters
// like requestPackageSize, ...
// -> via network config

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
            { LinkSimParams(1, 1) },
            listOf(update01)
        )
        val edgeGroupConfigs = listOf(EdgeGroupConfiguration(
            listOf(SoftwareState(software, 0, 0)),
            createPushStrategy(),
            { NodeSimParams(10) },
            { LinkSimParams(1, 1) },
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

    /*
    fun scenarioWithTwoUpdates(): Simulator.SimulationParams {
        // 1 size unit = 1 byte
        // 1 temp unit = 1 ms
        val software = Software("testSoftware")
        val update01 = SoftwareUpdate(software, 1, 100000) { 100 }
        val update02 = SoftwareUpdate(software, 2, 100000) { 100 }
        val edgeGroup = EdgeGroupConfiguration(
            listOf(SoftwareState(software, 0, 0)),
            UpdateRetrievalParams(registerAtServerForUpdates = true),
            //UpdateRetrievalParams(registerAtServerForUpdates = false, sendUpdateRequestsInterval = 3000),
            { NodeSimParams(1000000) },
            { LinkSimParams(1000, 20) },
        ) { level ->
            if (level == 6) {
                3
            } else if (level == 5) {
                0
            } else {
                0
            }
        }

        val scenario01Configuration = ScenarioConfiguration(
            6,
            2,
            ({ NodeSimParams(5000000) }),
            ({ LinkSimParams(1000, 20) }),
            listOf(update01, update02),
            listOf(edgeGroup)
        )
        return generateScenario(scenario01Configuration)
    }
     */
}
