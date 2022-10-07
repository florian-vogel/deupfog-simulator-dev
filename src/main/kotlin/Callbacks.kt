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

class MovePackageCallback(
    override val atInstant: Int,
    override val callbackParams: MovePackageCallbackParams
) : PackageStateChangeCallback {

    override fun runCallback() {
        movePackage(callbackParams.p, callbackParams.from, callbackParams.to)
    }

    private fun movePackage(p: Package, from: PackagePosition, to: PackagePosition) {
        from.remove(p)
        p.setPosition(to)
        val nextHop = Simulator.findNextHop(p)
        to.add(p, nextHop)
    }
}

data class MovePackageCallbackParams(
    val p: Package, val from: PackagePosition, val to: PackagePosition
) : PackageStateChangeCallbackParams(p)

class InitPackageCallback(
    override val atInstant: Int,
    override val callbackParams: InitPackageCallbackParams
) : PackageStateChangeCallback {

    override fun runCallback() {
        initPackage(callbackParams.p, callbackParams.atElement)
    }

    // auslagern in utility
    private fun initPackage(p: Package, atElement: PackagePosition) {
        val nextHop = Simulator.findNextHop(p)
        atElement.add(p, nextHop)
    }
}

data class InitPackageCallbackParams(
    val p: Package, val atElement: PackagePosition
) : PackageStateChangeCallbackParams(p)


