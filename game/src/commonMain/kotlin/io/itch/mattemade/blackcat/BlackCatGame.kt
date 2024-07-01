package io.itch.mattemade.blackcat

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readBitmapFont
import com.lehaine.littlekt.graphics.gl.TexMagFilter
import io.itch.mattemade.blackcat.assets.Assets
import io.itch.mattemade.blackcat.input.GameInput
import io.itch.mattemade.blackcat.input.bindInputs
import io.itch.mattemade.blackcat.scene.GameScene
import io.itch.mattemade.blackcat.scene.SlideshowScene
import io.itch.mattemade.utils.disposing.Disposing
import io.itch.mattemade.utils.disposing.Self

class BlackCatGame(
    context: Context,
    private val isSafari: Boolean
) : ContextListener(context), Disposing by Self() {

    private val controller = context.bindInputs()
    private val assets = Assets(context, ::handleAnimationSignal)
    private val slideshowScene = SlideshowScene(context, controller, assets)
    private var gameScene: GameScene? = null

    private fun handleAnimationSignal(signal: String) {
        if (gameScene == null) {
            return
        }
        when (signal) {
            "step" -> assets.sounds.step.next
            "jump" -> assets.sounds.jump
            "land" -> assets.sounds.land
            "climb" -> assets.sounds.climb.next
            "dash" -> assets.sounds.dash
            "death" -> assets.sounds.death.next
            else -> null
        }?.play()
    }


    override suspend fun Context.start() {

        val font = resourcesVfs["font/StoriaSans-Bold-120.fnt"].readBitmapFont(filter = TexMagFilter.LINEAR).disposing()
        val smallerFont =
            resourcesVfs["font/StoriaSans-Bold-60.fnt"].readBitmapFont(filter = TexMagFilter.LINEAR).disposing()

        var lastWidth = 0
        var lastHeight = 0

        fun launchGame(ignoreFirstResize: Boolean) {
            gameScene = GameScene(context, controller, font, smallerFont, isSafari, assets, ignoreFirstResize, ::handleAnimationSignal).apply {
                resize(lastWidth, lastHeight)
            }
        }
        slideshowScene.onFinish = { launchGame(ignoreFirstResize = true) }
        slideshowScene.smallerFont = smallerFont


        onResize { width, height ->
            lastWidth = width
            lastHeight = height
            gameScene?.apply {
                resize(width, height)
            } ?: run { slideshowScene.apply { resize(width, height) } }
        }
        onRender { dt ->
            if (controller.pressed(GameInput.RESTART)) {
                gameScene?.apply {
                    dispose()
                    launchGame(ignoreFirstResize = false)
                }
            }
            gameScene?.apply {
                render(dt)
            } ?: run { slideshowScene.apply { render(dt) } }
        }

        onDispose {
            slideshowScene.dispose()
            gameScene?.dispose()
            dispose()
        }
    }

}
