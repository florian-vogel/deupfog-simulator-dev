import java.util.*
import kotlin.collections.HashMap

/*
fun createSimpleTestPush(): Pair<Network, List<InitPackageCallback>> {
    val serverNode = Node(LinkedList(), 5)
    val receiverNode = Node(LinkedList(), 5)

    serverNode.addLink(UnidirectionalLinkPush(serverNode, receiverNode, LinkedList()))

    val simpleUpdatePackageSendCallbacks = listOf(
        InitPackageCallback(10, serverNode, receiverNode, 1, "Test Package 1"),
        InitPackageCallback(30, serverNode, receiverNode, 1, "Test Package 2"),
        InitPackageCallback(30, serverNode, receiverNode, 1, "Test Package 3"),
        InitPackageCallback(30, serverNode, receiverNode, 1, "Test Package 4"),
        InitPackageCallback(200, serverNode, receiverNode, 1, "Test Package 5")
    )

    return Pair(Network(), simpleUpdatePackageSendCallbacks)
}

fun createSimpleTestPush2(): Pair<Network, List<InitPackageCallback>> {
    val serverNode = Node(LinkedList(), 2)
    val intermediateServerNodePath1 = Node(LinkedList(), 2)
    val intermediateServerNodePath2 = Node(LinkedList(), 2)
    val receiverNodePath1 = Node(LinkedList(), 2)
    val receiverNodePath2 = Node(LinkedList(), 2)

    serverNode.addLink(UnidirectionalLinkPush(serverNode, intermediateServerNodePath1, LinkedList()))
    intermediateServerNodePath1.addLink(
        UnidirectionalLinkPush(
            intermediateServerNodePath1, receiverNodePath1, LinkedList()
        )
    )
    serverNode.addLink(UnidirectionalLinkPush(serverNode, intermediateServerNodePath2, LinkedList()))
    intermediateServerNodePath2.addLink(
        UnidirectionalLinkPush(
            intermediateServerNodePath2, receiverNodePath2, LinkedList()
        )
    )

    val simpleUpdatePackageSendCallbacks = listOf(
        InitPackageCallback(10, serverNode, receiverNodePath1, 1, "Test Package 1"),
        InitPackageCallback(20, serverNode, receiverNodePath2, 1, "Test Package 2")
    )

    return Pair(Network(), simpleUpdatePackageSendCallbacks)
}

fun createSimpleTestPull(): Pair<Network, List<InitPackageCallback>> {
    val serverNode = Node(LinkedList(), 10)
    val intermediateServerNodePath1 = Node(LinkedList(), 10)
    val intermediateServerNodePath2 = Node(LinkedList(), 10)
    val receiverNodePath1 = Node(LinkedList(), 10)
    val receiverNodePath2 = Node(LinkedList(), 10)

    serverNode.addLink(
        UnidirectionalLinkPull(
            serverNode, intermediateServerNodePath1, LinkedList()
        )
    )
    intermediateServerNodePath1.addLink(
        UnidirectionalLinkPull(
            intermediateServerNodePath1, receiverNodePath1, LinkedList()
        )
    )
    intermediateServerNodePath1.addLink(
        UnidirectionalLinkPush(
            intermediateServerNodePath1, serverNode, LinkedList()
        )
    )
    receiverNodePath1.addLink(
        UnidirectionalLinkPush(
            receiverNodePath1, intermediateServerNodePath1, LinkedList()
        )
    )

    serverNode.addLink(
        UnidirectionalLinkPull(
            serverNode, intermediateServerNodePath2, LinkedList()
        )
    )
    intermediateServerNodePath2.addLink(
        UnidirectionalLinkPull(
            intermediateServerNodePath2, receiverNodePath2, LinkedList()
        )
    )
    intermediateServerNodePath2.addLink(
        UnidirectionalLinkPush(
            intermediateServerNodePath2, serverNode, LinkedList()
        )
    )
    receiverNodePath2.addLink(
        UnidirectionalLinkPush(
            receiverNodePath2, intermediateServerNodePath2, LinkedList()
        )
    )

    val simpleUpdatePackageSendCallbacks = listOf(
        InitPackageCallback(10, serverNode, receiverNodePath1, 1, "Test Package 1"),
        InitPackageCallback(20, serverNode, receiverNodePath2, 1, "Test Package 2")
    )

    return Pair(Network(), simpleUpdatePackageSendCallbacks)
}
*/
fun createSimpleTestWithSpecificSoftwareVersions(): List<InitPackageCallback> {
    val software1 = Software("software1")
    val newVersion = SoftwareVersion(software1, 1, 1)
    val initialVersion = SoftwareVersion(software1, 0, 1)

    val serverNode = Server(LinkedList(), 5)
    val serverNode2 = Server(LinkedList(), 5)
    val receiverNode = EdgeNode(
        LinkedList(), 5, hashMapOf(software1 to initialVersion) as HashMap<Software, SoftwareVersion>, serverNode
    )
    val receiverNode2 = EdgeNode(
        LinkedList(), 5, hashMapOf(software1 to initialVersion) as HashMap<Software, SoftwareVersion>, serverNode2
    )

    serverNode.addLink(UnidirectionalLinkPush(serverNode, serverNode2, LinkedList()))
    serverNode2.addLink(UnidirectionalLinkPush(serverNode2, receiverNode, LinkedList()))
    serverNode2.addLink(UnidirectionalLinkPull(serverNode2, receiverNode2, LinkedList()))
    receiverNode2.addLink(UnidirectionalLinkPush(receiverNode2, serverNode2, LinkedList()))
    serverNode2.registerNodeInRegistry(receiverNode)
    serverNode2.registerNodeInRegistry(receiverNode2)
    serverNode.registerNodeInRegistry(serverNode2)

    val simpleUpdatePackageSendCallbacks = listOf(
        InitUpdatePackageCallback(10, serverNode, newVersion, "update to new version")
    )

    return simpleUpdatePackageSendCallbacks
}