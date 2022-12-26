package Software

class SoftwareUpdate(
    val type: Software,
    val updatesToVersion: Int,
    val size: Int,
    val newSoftwareSize: (oldSize: Int) -> Int,
)