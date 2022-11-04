data class TimedCallback(val instant: Int, val callback: (sim: Simulator) -> Unit)

// assume for now that our network is static -> the only callback that can occur is a move package callback
interface PackageStateChangeCallback {
    val atInstant: Int
    val callbackParams: PackageStateChangeCallbackParams
    fun runCallback() {}
}

open class PackageStateChangeCallbackParams(
    p: Package
)

class PackageArriveCallback(
    override val atInstant: Int,
    override val callbackParams: PackageArriveCallbackParams
) : PackageStateChangeCallback {

    override fun runCallback() {
        movePackage(
            callbackParams.p,
            callbackParams.p.getPosition(),
            callbackParams.via.getDestination(),
            callbackParams.via
        )
    }

    // TODO: move to utilities
    private fun movePackage(p: Package, from: Node, to: Node, via: UnidirectionalLink) {
        from.arrivedVia(via)
        p.setPosition(to)
        val nextHop = Simulator.findNextHop(p)
        to.receive(p, nextHop)
    }
}

data class PackageArriveCallbackParams(
    val p: Package, val via: UnidirectionalLink
) : PackageStateChangeCallbackParams(p)

class InitPackageCallback(
    override val atInstant: Int,
    override val callbackParams: InitPackageCallbackParams,
) : PackageStateChangeCallback {

    override fun runCallback() {
        addPackageAt(callbackParams.p)
        if (callbackParams.repeat !== null) {
            val requestPackage = RequestPackage(callbackParams.p.getPosition(), callbackParams.p.getDestination(), 1)
            Simulator.addCallback(
                InitPackageCallback(
                    Simulator.getCurrentTimestamp() + callbackParams.repeat,
                    InitPackageCallbackParams(requestPackage, callbackParams.repeat)
                )
            )
        }
    }

    // TODO: move to utilities
    private fun addPackageAt(p: Package) {
        val nextHop = Simulator.findNextHop(p)
        p.getInitialPosition().receive(p, nextHop)
    }
}

data class InitPackageCallbackParams(
    val p: Package, val repeat: Int? = null
) : PackageStateChangeCallbackParams(p)
