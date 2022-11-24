interface TimedCallback {
    val atInstant: Int
    fun runCallback()
    fun cancelCallback()
}

// TODO: differenciate between finish sending and arriving? -> maybe not so important since the propagation delay in local networks is neglectable
class CompleteTransmissionCallback(
    override val atInstant: Int, private val transmission: Transmission
) : TimedCallback {
    override fun runCallback() {
        transmission.completeTransmitting()
    }

    // TODO: not used currently
    override fun cancelCallback() {
        transmission.cancelTransmitting()
    }
}

open class InitPackageAtNodeCallback(
    override val atInstant: Int, private val p: Package
) : TimedCallback {
    override fun runCallback() {
        p.initialPosition.receive(p)
    }

    override fun cancelCallback() {}
}

open class RecursiveCallback(
    override val atInstant: Int,
    private val endInstant: Int?,
    private val interval: Int,
    private val customCallback: (() -> Unit)
) : TimedCallback {
    override fun runCallback() {
        if (endInstant == null || atInstant <= endInstant) {
            customCallback()
            val nextCallInstant = atInstant + interval
            Simulator.addCallback(RecursiveCallback(nextCallInstant, endInstant, interval, customCallback))
        }
    }

    override fun cancelCallback() {
        TODO("Not yet implemented")
    }
}
