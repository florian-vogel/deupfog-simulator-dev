class Software(val name: String) {

    fun updateCompatible(fromVersion: Int, toVersion: Int): Boolean {
        return updatesOnlyOneVersion(fromVersion, toVersion)
    }

    private fun updatesOnlyOneVersion(fromVersion: Int, toVersion: Int): Boolean {
        return fromVersion + 1 == toVersion
    }
}

// TODO: rename to smt like software state
class SoftwareState(val type: Software, var versionNumber: Int, var size: Int) {
    fun applyUpdate(update: SoftwareUpdate) {
        if (type.updateCompatible(versionNumber, update.updatesToVersion)) {
            versionNumber = update.updatesToVersion
            size = update.newSoftwareSize(size)
        }
    }
}

class SoftwareUpdate(
    val type: Software,
    val updatesToVersion: Int,
    val size: Int,
    val initializeTimestamp: Int,
    val newSoftwareSize: (oldSize: Int) -> Int,
)