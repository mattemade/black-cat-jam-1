package io.itch.mattemade.blackcat

import com.lehaine.littlekt.createLittleKtApp
import com.lehaine.littlekt.graphics.Color

fun main() {
    createLittleKtApp {
        width = 1920
        height = 1080
        backgroundColor = Color.DARK_GRAY
        title = "Black Cat Game"
    }.start {
        BlackCatGame(it, safePlayClip = { play() }, safePlayStream = { play(loop = it) })
    }
}