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

// note bandwidth as bytes per milllisecond
val lowResourceLinkConfig = LinkConfig(6, 100, tcpTransmissionConfig)
val highResourceLinkConfig = LinkConfig(20, 50, tcpTransmissionConfig)

val lowResourceNodeConfig = NodeConfig(8000000, infrequentOfflineBehaviour)
val highResourceNodeConfig = NodeConfig(8000000)
val serverNodeConfig = NodeConfig(500000000)
