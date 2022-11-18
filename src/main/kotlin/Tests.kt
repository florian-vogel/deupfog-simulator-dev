import java.util.*
import kotlin.collections.HashMap

fun createSimpleTest(metrics: MetricsCollector<Software>): List<InitPackageAtNodeCallback> {
    val software = Software("software")
    val serverNode = Server(mutableListOf(), 10, listOf(), listOf(software), UpdateRetrievalParams())
    val edgeNode = Edge(mutableListOf(), 10, listOf(serverNode), listOf(software), UpdateRetrievalParams(true))

    val link = UnidirectionalLink(serverNode, edgeNode)
    serverNode.addLink(link)

    val update = SoftwareUpdate(software, 2, 1)
    val p = UpdateResponse(serverNode, serverNode, 1, update)


    return listOf(InitPackageAtNodeCallback(0, p))
}