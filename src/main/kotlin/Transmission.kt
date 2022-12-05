interface Transmission {
    val p: Package
    val transmissionTime: Int
    val via: UnidirectionalLink

    fun startTransmitting()
    fun completeTransmitting()
    fun cancelTransmitting()
}

// TODO: calculate transmission time here
class SimpleTransmission(
    override val p: Package, override val transmissionTime: Int, override val via: UnidirectionalLink
) : Transmission {
    init {
        startTransmitting()
    }

    override fun startTransmitting() {
        val callback = TimedCallback(Simulator.getCurrentTimestamp() + transmissionTime) { completeTransmitting() }
        Simulator.addCallback(callback)
    }

    override fun completeTransmitting() {
        via.to.receive(p)
        via.transmissionFinished()
    }

    override fun cancelTransmitting() {
        via.transmissionFinished()
    }
}
