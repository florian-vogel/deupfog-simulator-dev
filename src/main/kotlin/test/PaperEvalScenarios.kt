package test

import network.*
import simulator.InitialUpdateParams
import simulator.SimulationSetup
import software.Software
import software.SoftwareState
import software.SoftwareUpdate


class PaperEvalScenarios {
    fun scenario01(): SimulationSetup {
        val updateDisseminationStrategy = createPushStrategy()

        val updateInitTimestamp = 1000
        val updateSize = 50000

        val brokerTopologyLevels = 3
        val brokerTopologyBranchingFactor = 4
        val edgesPerBrokerOnTheLowestLevel = 16
        val interBrokerTopologyLinkConfig = highResourceLinkConfig
        val brokerNodeConfig = highResourceNodeConfig

        val edgeLinkConfig = lowResourceLinkConfig
        val edgeNodeConfig = lowResourceNodeConfig

        val software = Software("testSoftware")
        val update01 = SoftwareUpdate(software, 1, updateSize) { 0 }

        val networkConfig = NetworkConfig(
            updateDisseminationStrategy,
            listOf(software),
            mqttPackageConfig
        )
        val hierarchyConfig = HierarchyConfiguration(
            brokerTopologyLevels,
            brokerTopologyBranchingFactor,
            { brokerNodeConfig },
            { interBrokerTopologyLinkConfig },
        )
        val edgeGroupConfigs = listOf(EdgeGroupConfiguration(
            listOf(SoftwareState(software, 0, 0)),
            updateDisseminationStrategy,
            { edgeNodeConfig },
            { edgeLinkConfig },
            { level -> if (level == brokerTopologyLevels) edgesPerBrokerOnTheLowestLevel else 0 }
        ))
        val network = generateHierarchicalNetwork(
            networkConfig,
            hierarchyConfig,
            edgeGroupConfigs
        )
        val updateParams =
            listOf(
                InitialUpdateParams(update01, updateInitTimestamp, network.updateInitializationServers),
            )
        return SimulationSetup(network, updateParams)
    }

    fun scenario02(): SimulationSetup {
        val updateDisseminationStrategy = createPullStrategy(5000)

        val updateInitTimestamp = 1000
        val updateSize = 50000

        val brokerTopologyLevels = 0
        val brokerTopologyBranchingFactor = 0
        val edgesPerBrokerOnTheLowestLevel = 1024
        val interBrokerTopologyLinkConfig = highResourceLinkConfig
        val brokerNodeConfig = serverNodeConfig

        val edgeLinkConfig = lowResourceLinkConfig
        val edgeNodeConfig = lowResourceNodeConfig

        val software = Software("testSoftware")
        val update01 = SoftwareUpdate(software, 1, updateSize) { 0 }

        val networkConfig = NetworkConfig(
            updateDisseminationStrategy,
            listOf(software),
            mqttPackageConfig
        )
        val hierarchyConfig = HierarchyConfiguration(
            brokerTopologyLevels,
            brokerTopologyBranchingFactor,
            { brokerNodeConfig },
            { interBrokerTopologyLinkConfig },
        )
        val edgeGroupConfigs = listOf(EdgeGroupConfiguration(
            listOf(SoftwareState(software, 0, 0)),
            updateDisseminationStrategy,
            { edgeNodeConfig },
            { edgeLinkConfig },
            { level -> if (level == brokerTopologyLevels) edgesPerBrokerOnTheLowestLevel else 0 }
        ))
        val network = generateHierarchicalNetwork(
            networkConfig,
            hierarchyConfig,
            edgeGroupConfigs
        )
        val updateParams =
            listOf(
                InitialUpdateParams(update01, updateInitTimestamp, network.updateInitializationServers),
            )
        return SimulationSetup(network, updateParams)
    }
}
