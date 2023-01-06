package software

// todo:
// rename to artifact or similar
class Software(val name: String) {

    fun updateCompatible(state: SoftwareState, update: SoftwareUpdate): Boolean {
        return updatesOnlyOneVersion(state.versionNumber, update.updatesToVersion)
    }

    private fun updatesOnlyOneVersion(fromVersion: Int, toVersion: Int): Boolean {
        return fromVersion + 1 == toVersion
    }
}
