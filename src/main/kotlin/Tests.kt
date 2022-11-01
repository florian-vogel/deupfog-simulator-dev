import java.util.*

fun createSimpleTest(): Pair<Network, List<InitPackageCallback>> {
    val serverNode = Node(LinkedList(), 2)
    val intermediateServerNodePath1 = Node(LinkedList(), 2)
    val intermediateServerNodePath2 = Node(LinkedList(), 2)
    val receiverNodePath1 = Node(LinkedList(), 2)
    val receiverNodePath2 = Node(LinkedList(), 2)

    serverNode.addLink(UnidirectionalLinkPush(intermediateServerNodePath1, LinkedList()))
    intermediateServerNodePath1.addLink(UnidirectionalLinkPush(receiverNodePath1, LinkedList()))
    serverNode.addLink(UnidirectionalLinkPush(intermediateServerNodePath2, LinkedList()))
    intermediateServerNodePath2.addLink(UnidirectionalLinkPush(receiverNodePath2, LinkedList()))

    val p1 = Package(serverNode, receiverNodePath1, 1)
    val p2 = Package(serverNode, receiverNodePath2, 1)
    val simpleUpdatePackageSendCallbacks = listOf(
        InitPackageCallback(10, InitPackageCallbackParams(p1, serverNode)),
        InitPackageCallback(20, InitPackageCallbackParams(p2, serverNode))
    )

    return Pair(Network(), simpleUpdatePackageSendCallbacks)
}
