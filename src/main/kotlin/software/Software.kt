package software

// todo:
// rename to artifact or similar
class Software(val name: String) {

    fun updateCompatible(fromVersion: Int, toVersion: Int): Boolean {
        return updatesOnlyOneVersion(fromVersion, toVersion)
    }

    private fun updatesOnlyOneVersion(fromVersion: Int, toVersion: Int): Boolean {
        return fromVersion + 1 == toVersion
    }
}

fun applyUpdates(type: Software, availableUpdates: List<SoftwareUpdate>): SoftwareState {
    val state = SoftwareState(type, 0, 0)
    availableUpdates.toMutableList().sortBy { update -> update.updatesToVersion }
    availableUpdates.forEach { update -> state.applyUpdate(update) }
    return state
}