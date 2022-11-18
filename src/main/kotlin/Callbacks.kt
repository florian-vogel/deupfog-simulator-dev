interface TimedCallback {
    val atInstant: Int
    fun runCallback()
    fun cancelCallback()
}

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