class MetricsCollector(val includeRequestPackage: Boolean = false) {
    val linkMetricsCollector = LinkMetricsCollector()
    val packageMetricsCollector = PackageMetricsCollector(includeRequestPackage)

    fun printMetrics() {
        this.linkMetricsCollector.printMetrics()
        this.packageMetricsCollector.printMetrics()
    }
}

class LinkMetricsCollector() {
    class MetricsPerLink() {
        var timeOccupied: Int = 0

        private var isTransferring: Boolean = false
        private var timestampAtLastIsTransferringChange = Simulator.getCurrentTimestamp()

        fun onStartTransfer() {
            if (!isTransferring) {
                isTransferring = true
                timestampAtLastIsTransferringChange = Simulator.getCurrentTimestamp()
            }
        }

        fun onStopTransfer() {
            if (isTransferring) {
                isTransferring = false
                timeOccupied += Simulator.getCurrentTimestamp() - timestampAtLastIsTransferringChange
                timestampAtLastIsTransferringChange = Simulator.getCurrentTimestamp()
            }
        }
    }

    private val metrics: MutableMap<UnidirectionalLink, MetricsPerLink> = LinkedHashMap()

    fun register(link: UnidirectionalLink) {
        metrics[link] = MetricsPerLink()
    }

    fun getMetricsCollectorForLink(link: UnidirectionalLink): MetricsPerLink? {
        return metrics[link]
    }

    fun printMetrics() {
        this.metrics.forEach {
            println("link: ${it.key}, timeOccupied: ${it.value.timeOccupied}")
        }
    }
}

class PackageMetricsCollector(val includeRequestPackage: Boolean) {
    class MetricsPerPackage() {
        var timeToReachDestination: Int? = null
        var timeWaiting: Int = 0
        var createTimestamp = Simulator.getCurrentTimestamp()
        var arriveTimestamp: Int? = null
        private var isWaiting = false
        private var timestampAtLastIsWaitingChange = Simulator.getCurrentTimestamp()

        fun onArrive() {
            timeToReachDestination = Simulator.getCurrentTimestamp() - createTimestamp
            arriveTimestamp = Simulator.getCurrentTimestamp()
        }

        fun onAddedToQueue() {
            if (!isWaiting) {
                isWaiting = true
                timestampAtLastIsWaitingChange = Simulator.getCurrentTimestamp()
            }
        }

        fun onStartTransfer() {
            if (isWaiting) {
                isWaiting = false
                timeWaiting += Simulator.getCurrentTimestamp() - timestampAtLastIsWaitingChange
                timestampAtLastIsWaitingChange = Simulator.getCurrentTimestamp()
            }
        }
    }

    private val metrics: MutableMap<Package, MetricsPerPackage> = LinkedHashMap()

    fun register(p: Package) {
        metrics[p] = MetricsPerPackage()
    }

    fun getMetricsCollector(p: Package): MetricsPerPackage? {
        return metrics[p]
    }

    fun printMetrics() {
        this.metrics.forEach {
            if (it.key !is RequestPackage || includeRequestPackage) {
                println(
                    "package: ${it.key}, " +
                            "timeToReachDestination: ${it.value.timeToReachDestination}, " +
                            "timeWaiting: ${it.value.timeWaiting}, " +
                            "createTimestamp: ${it.value.createTimestamp}, " +
                            "arriveTimestamp: ${it.value.arriveTimestamp}"
                )
            }
        }
    }
}
