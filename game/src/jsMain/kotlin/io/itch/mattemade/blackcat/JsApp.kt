package io.itch.mattemade.blackcat

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.RemoveContextCallback
import com.lehaine.littlekt.audio.AudioContext
import com.lehaine.littlekt.createLittleKtApp
import com.lehaine.littlekt.graphics.Color
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement

private const val CANVAS_ID = "littleKtGameCanvas"

external fun applySafariAudioHack()
external fun getOrCreateAudioListener(): AudioContext

fun main() {
    createLittleKtApp {
        title = "Black Cat Game"
        backgroundColor = Color.DARK_GRAY
        canvasId = CANVAS_ID
    }.start { context ->
        scheduleCanvasResize(context)
        applySafariAudioHack()

        BlackCatGame(context)

    }
}

private fun scheduleCanvasResize(context: Context) {
    var removeContextCallback: RemoveContextCallback? = null
    removeContextCallback = context.onRender {
        val canvas = document.getElementById(CANVAS_ID) as HTMLElement
        canvas.style.apply {
            display = "block"
            position = "absolute"
            top = "0"
            bottom = "0"
            left = "0"
            right = "0"
            width = "100%"
            height = "100%"
        }
        removeContextCallback?.invoke()
        removeContextCallback = null
    }
}