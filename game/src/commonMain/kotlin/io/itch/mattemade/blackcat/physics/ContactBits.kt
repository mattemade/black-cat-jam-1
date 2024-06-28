package io.itch.mattemade.blackcat.physics

object ContactBits {
    const val CAT_BIT = 1
    const val GRASS_BIT = 1 shl 1
    const val BRANCH_BIT = 1 shl 2
    const val WATER_BIT = 1 shl 3
    const val BLOCK_BIT = 1 shl 4
    const val PLATFORM_BIT = 1 shl 5
    const val WALL_BIT = 1 shl 6

    const val ALL_NORMAL_BITS = CAT_BIT or GRASS_BIT or BRANCH_BIT or WATER_BIT or BLOCK_BIT

    const val CAMERA_BIT = 1 shl 30
}