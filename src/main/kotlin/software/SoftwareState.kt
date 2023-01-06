package software

class SoftwareState(val type: Software, val versionNumber: Int, val size: Int) {
    fun applyUpdate(update: SoftwareUpdate): SoftwareState {
        if (type.updateCompatible(this, update)) {
            return SoftwareState(
                type, update.updatesToVersion, update.newSoftwareSize(size)
            )
        } else {
            return this
        }
    }

    fun isEqual(state: SoftwareState): Boolean {
        return state.type == type && state.versionNumber == versionNumber && state.size == size
    }
}

fun applyUpdates(type: Software, availableUpdates: List<SoftwareUpdate>): SoftwareState {
    var state = SoftwareState(type, 0, 0)
    availableUpdates.toMutableList().sortBy { update -> update.updatesToVersion }
    availableUpdates.forEach { update -> state = state.applyUpdate(update) }
    return state
}
