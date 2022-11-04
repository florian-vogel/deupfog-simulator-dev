import java.util.*

fun createSimpleTestPush(): Pair<Network, List<InitPackageCallback>> {
    val serverNode = Node(LinkedList(), 2)
    val intermediateServerNodePath1 = Node(LinkedList(), 2)
    val intermediateServerNodePath2 = Node(LinkedList(), 2)
    val receiverNodePath1 = Node(LinkedList(), 2)
    val receiverNodePath2 = Node(LinkedList(), 2)

    serverNode.addLink(UnidirectionalLinkPush(serverNode, intermediateServerNodePath1, LinkedList()))
    intermediateServerNodePath1.addLink(
        UnidirectionalLinkPush(
            intermediateServerNodePath1,
            receiverNodePath1,
            LinkedList()
        )
    )
    serverNode.addLink(UnidirectionalLinkPush(serverNode, intermediateServerNodePath2, LinkedList()))
    intermediateServerNodePath2.addLink(
        UnidirectionalLinkPush(
            intermediateServerNodePath2,
            receiverNodePath2,
            LinkedList()
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

    serverNode.addLink(UnidirectionalLinkPull(serverNode, intermediateServerNodePath1, LinkedList()))
    intermediateServerNodePath1.addLink(
        UnidirectionalLinkPull(
            intermediateServerNodePath1,
            receiverNodePath1,
            LinkedList()
        )
    )
    intermediateServerNodePath1.addLink(UnidirectionalLinkPush(intermediateServerNodePath1, serverNode, LinkedList()))
    receiverNodePath1.addLink(
        UnidirectionalLinkPush(
            receiverNodePath1,
            intermediateServerNodePath1,
            LinkedList()
        )
    )

    serverNode.addLink(UnidirectionalLinkPull(serverNode, intermediateServerNodePath2, LinkedList()))
    intermediateServerNodePath2.addLink(
        UnidirectionalLinkPull(
            intermediateServerNodePath2,
            receiverNodePath2,
            LinkedList()
        )
    )
    intermediateServerNodePath2.addLink(UnidirectionalLinkPush(intermediateServerNodePath2, serverNode, LinkedList()))
    receiverNodePath2.addLink(
        UnidirectionalLinkPush(
            receiverNodePath2,
            intermediateServerNodePath2,
            LinkedList()
        )
    )

    val simpleUpdatePackageSendCallbacks = listOf(
        InitPackageCallback(10, serverNode, receiverNodePath1, 1, "Test Package 1"),
        InitPackageCallback(20, serverNode, receiverNodePath2, 1, "Test Package 2")
    )

    return Pair(Network(), simpleUpdatePackageSendCallbacks)
}
