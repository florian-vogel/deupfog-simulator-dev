package test

import network.*
import simulator.InitialUpdateParams
import simulator.SimulationSetup
import simulator.Simulator
import simulator.SimulatorConfig
import software.Software
import software.SoftwareState
import software.SoftwareUpdate

val updateInitTimestamp = 3000
val updateSize = 50000

val software = Software("testSoftware")
val update01 = SoftwareUpdate(software, 1, updateSize) { 0 }

// client server
val edgePullInterval = 5000
val randomEdgeLinkConfig = lowResourceRandomLinkConfig
val serverNodeConfig = veryHighResourceNodeConfig
val totalEdgeCount = 512

// pub sub
val brokerTopologyLevels = 3
val brokerTopologyBranchingFactor = 4
val edgesPerBrokerOnTheLowestLevel = 8
val interBrokerTopologyLinkConfig = highResourceLinkConfig
val brokerNodeConfig = highResourceNodeConfig

val edgeLinkConfig = lowResourceLinkConfig
val edgeNodeConfig = lowResourceNodeConfig

class PaperEvalScenarios {
    fun runAll() {
        val pushRefSimSetup = PaperEvalScenarios().scenarioPushReference()
        val pushRefSim = Simulator(pushRefSimSetup, SimulatorConfig("pushReference", 60000))
        pushRefSim.runSimulation()

        val pullRefSimSetup = PaperEvalScenarios().scenarioPullReference()
        val pullRefSim = Simulator(pullRefSimSetup, SimulatorConfig("pullReference", 60000))
        pullRefSim.runSimulation()

        val pushLargeSimSetup = PaperEvalScenarios().scenarioPushLarge()
        val pushLargeSim = Simulator(pushLargeSimSetup, SimulatorConfig("pushLarge", 60000))
        pushLargeSim.runSimulation()
        val pullLargeSimSetup = PaperEvalScenarios().scenarioPullLarge()
        val pullLargeSim = Simulator(pullLargeSimSetup, SimulatorConfig("pullLarge", 60000))
        pullLargeSim.runSimulation()

        val pushUnreliableSimSetup = PaperEvalScenarios().scenarioPushUnreliable()
        val pushUnreliableSim = Simulator(pushUnreliableSimSetup, SimulatorConfig("pushUnreliable", 60000))
        pushUnreliableSim.runSimulation()
        val pullUnreliableSimSetup = PaperEvalScenarios().scenarioPullUnreliable()
        val pullUnreliableSim = Simulator(pullUnreliableSimSetup, SimulatorConfig("pullUnreliable", 60000))
        pullUnreliableSim.runSimulation()
    }

    fun scenarioPushReference(): SimulationSetup {
        val updateDisseminationStrategy = createPushStrategy()

        val networkConfig = NetworkConfig(
            updateDisseminationStrategy,
            listOf(software),
            mqttPackageConfig
        )
        val hierarchyConfig = HierarchyConfiguration(
            brokerTopologyLevels,
            brokerTopologyBranchingFactor,
            { brokerNodeConfig },
            { interBrokerTopologyLinkConfig() },
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

    fun scenarioPullReference(): SimulationSetup {
        val updateDisseminationStrategy = createPullStrategy(edgePullInterval)

        val networkConfig = NetworkConfig(
            updateDisseminationStrategy,
            listOf(software),
            mqttPackageConfig
        )
        val clientServerNetwork = generateClientServerNetwork(
            ClientServerConfiguration(
                networkConfig,
                serverNodeConfig,
                totalEdgeCount,
                edgeNodeConfig,
                randomEdgeLinkConfig,
                listOf(SoftwareState(software, 0, 0)),
                edgePullInterval
            )
        )
        val updateParams =
            listOf(
                InitialUpdateParams(update01, updateInitTimestamp, clientServerNetwork.updateInitializationServers),
            )
        return SimulationSetup(clientServerNetwork, updateParams)
    }

    fun scenarioPushLarge(): SimulationSetup {
        val updateDisseminationStrategy = createPushStrategy()
        val edgesPerBrokerOnTheLowestLevelOverride = 16

        val networkConfig = NetworkConfig(
            updateDisseminationStrategy,
            listOf(software),
            mqttPackageConfig
        )
        val hierarchyConfig = HierarchyConfiguration(
            brokerTopologyLevels,
            brokerTopologyBranchingFactor,
            { brokerNodeConfig },
            { interBrokerTopologyLinkConfig() },
        )
        val edgeGroupConfigs = listOf(EdgeGroupConfiguration(
            listOf(SoftwareState(software, 0, 0)),
            updateDisseminationStrategy,
            { edgeNodeConfig },
            { edgeLinkConfig },
            { level -> if (level == brokerTopologyLevels) edgesPerBrokerOnTheLowestLevelOverride else 0 }
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

    fun scenarioPullLarge(): SimulationSetup {
        val totalEdgeCountLarge = 1024
        val updateDisseminationStrategy = createPullStrategy(edgePullInterval)

        val networkConfig = NetworkConfig(
            updateDisseminationStrategy,
            listOf(software),
            mqttPackageConfig
        )
        val clientServerNetwork = generateClientServerNetwork(
            ClientServerConfiguration(
                networkConfig,
                serverNodeConfig,
                totalEdgeCountLarge,
                edgeNodeConfig,
                randomEdgeLinkConfig,
                listOf(SoftwareState(software, 0, 0)),
                edgePullInterval
            )
        )
        val updateParams =
            listOf(
                InitialUpdateParams(update01, updateInitTimestamp, clientServerNetwork.updateInitializationServers),
            )
        return SimulationSetup(clientServerNetwork, updateParams)
    }

    fun scenarioPushUnreliable(): SimulationSetup {
        val updateDisseminationStrategy = createPushStrategy()

        val edgeNodeConfigOverride = lowResourceNodeConfigUnreliable

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
            { interBrokerTopologyLinkConfig() },
        )
        val edgeGroupConfigs = listOf(EdgeGroupConfiguration(
            listOf(SoftwareState(software, 0, 0)),
            updateDisseminationStrategy,
            { edgeNodeConfigOverride },
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

    fun scenarioPullUnreliable(): SimulationSetup {
        val updateDisseminationStrategy = createPullStrategy(edgePullInterval)

        val edgeNodeConfigUnreliable = lowResourceNodeConfigUnreliable

        val networkConfig = NetworkConfig(
            updateDisseminationStrategy,
            listOf(software),
            mqttPackageConfig
        )
        val clientServerNetwork = generateClientServerNetwork(
            ClientServerConfiguration(
                networkConfig,
                serverNodeConfig,
                totalEdgeCount,
                edgeNodeConfigUnreliable,
                randomEdgeLinkConfig,
                listOf(SoftwareState(software, 0, 0)),
                edgePullInterval
            )
        )
        val updateParams =
            listOf(
                InitialUpdateParams(update01, updateInitTimestamp, clientServerNetwork.updateInitializationServers),
            )
        return SimulationSetup(clientServerNetwork, updateParams)
    }
}