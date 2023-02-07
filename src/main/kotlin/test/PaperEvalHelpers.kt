package test

import network.LinkConfig
import network.TransmissionConfig
import network.createRandomOfflineBehaviour
import node.NodeConfig
import node.PackagesConfigServer

// time values in ms
// size values in byte

val tcpTransmissionConfig = TransmissionConfig(
    4
) { size, bandwidth, delay -> size / bandwidth + delay * 2 }

val mqttPackageConfig = PackagesConfigServer(
    20, 20, 20
) { _, _ -> 10 + (1..100).random() }

val frequentOfflineBehaviour = createRandomOfflineBehaviour(10000, 10000)
val infrequentOfflineBehaviour = createRandomOfflineBehaviour(30000, 10000)

// note bandwidth as bytes per ms
val lowResourceLinkConfig = { LinkConfig(6, 100, tcpTransmissionConfig) }
val highResourceLinkConfig = { LinkConfig(20, 50, tcpTransmissionConfig) }
val randomLinkConfig = { bandwidthLow: Int, bandwidthHigh: Int, latencyLow: Int, latencyHigh: Int ->
    {
        LinkConfig(
            (bandwidthLow..bandwidthHigh).random(),
            (latencyLow..latencyHigh).random(),
            tcpTransmissionConfig
        )
    }
}
val lowResourceRandomLinkConfig = randomLinkConfig(3, 9, 50, 150)

val lowResourceNodeConfig = NodeConfig(3000000, infrequentOfflineBehaviour)
val lowResourceNodeConfigUnreliable = NodeConfig(3000000, frequentOfflineBehaviour)
val highResourceNodeConfig = NodeConfig(3000000)
val veryHighResourceNodeConfig = NodeConfig(30000000)

