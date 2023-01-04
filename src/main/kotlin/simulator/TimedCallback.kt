package simulator

open class TimedCallback(val atInstant: Int, private val callback: () -> Unit) {
    fun runCallback() {
        this.callback()
    }
}
