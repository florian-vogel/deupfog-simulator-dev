class Package(override val initialPosition: PackagePosition, val destination: PackagePosition, val size: Int) :
    PositionablePackage {
    private var currentPosition: PackagePosition = initialPosition
    override fun setPosition(newPosition: PackagePosition) {
        currentPosition = newPosition
    }

    override fun getPosition(): PackagePosition {
        return currentPosition
    }
}

