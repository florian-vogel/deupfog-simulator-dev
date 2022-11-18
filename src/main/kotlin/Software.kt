open class UpdatableState<TUpdatable : UpdatableType>(
    val type: TUpdatable, var versionNumber: Int = 0
) {
    fun applyUpdate(update: UpdatableUpdate<TUpdatable>) {
        if (type.updateCompatible(versionNumber, update.updatesToVersion)) {
            versionNumber = update.updatesToVersion
        }
    }
}

interface UpdatableType {
    val name: String

    fun updateCompatible(fromVersion: Int, toVersion: Int): Boolean
}

interface UpdatableUpdate<TUpdatable : UpdatableType> {
    val type: TUpdatable
    val updatesToVersion: Int
    val size: Int
}

class Software(override val name: String) : UpdatableType {

    override fun updateCompatible(fromVersion: Int, toVersion: Int): Boolean {
        return updatesOnlyOneVersion(fromVersion, toVersion)
    }

    private fun updatesOnlyOneVersion(fromVersion: Int, toVersion: Int): Boolean {
        return fromVersion + 1 == toVersion
    }
}

class RunningSoftware(type: Software, versionNumber: Int, val size: Int) : UpdatableState<Software>(type, versionNumber)

class SoftwareUpdate(
    override val type: Software,
    override val updatesToVersion: Int,
    override val size: Int
) :
    UpdatableUpdate<Software> {
    init {
        Simulator.getUpdateMetrics()?.registerUpdate(this)
        println("create upate with metrics: ${Simulator.getUpdateMetrics()}")
    }
}