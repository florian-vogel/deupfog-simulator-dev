package software

class SoftwareState(val type: Software, var versionNumber: Int, var size: Int) {
    fun applyUpdate(update: SoftwareUpdate) {
        if (type.updateCompatible(versionNumber, update.updatesToVersion)) {
            versionNumber = update.updatesToVersion
            size = update.newSoftwareSize(size)
        }
    }
}
