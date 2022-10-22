import java.util.*
import kotlin.collections.HashMap

fun createSimpleTest(): Pair<Network, List<InitPackageCallback>> {
    val nodes = LinkedList<Node>()
    val serverNode = Node(LinkedList(), 1)
    nodes.add(serverNode)
    val intermediateNode = Node(LinkedList(), 1)
    nodes.add(intermediateNode)
    val receiverNode = Node(LinkedList(), 1)
    nodes.add(receiverNode)

    serverNode.addLink(UnidirectionalLinkPush(intermediateNode))
    intermediateNode.addLink(UnidirectionalLinkPush(receiverNode))
    val simpleNetwork = Network(nodes)

    val p1 = Package(serverNode, receiverNode, 1)
    val simpleUpdatePackageSendCallbacks = listOf(InitPackageCallback(10, InitPackageCallbackParams(p1, serverNode)))

    return Pair(simpleNetwork, simpleUpdatePackageSendCallbacks)
}
