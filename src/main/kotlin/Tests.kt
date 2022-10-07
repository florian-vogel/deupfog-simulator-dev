import java.util.*
import kotlin.collections.HashMap

fun createSimpleTest(): Pair<Network, List<InitPackageCallback>> {
    val nodes = LinkedList<Node>()
    val serverNode = Node(HashMap(), 1)
    nodes.add(serverNode)
    val intermediateNode = Node(HashMap(), 1)
    nodes.add(intermediateNode)
    val receiverNode = Node(HashMap(), 1)
    nodes.add(receiverNode)

    serverNode.linksTo[intermediateNode] = UnidirectionalLinkPush(intermediateNode)
    intermediateNode.linksTo[receiverNode] = UnidirectionalLinkPush(receiverNode)
    val simpleNetwork = Network(nodes)

    val p1 = Package(serverNode, receiverNode, 1)
    val simpleUpdatePackageSendCallbacks = listOf(InitPackageCallback(10, InitPackageCallbackParams(p1, serverNode)))

    return Pair(simpleNetwork, simpleUpdatePackageSendCallbacks)
}
