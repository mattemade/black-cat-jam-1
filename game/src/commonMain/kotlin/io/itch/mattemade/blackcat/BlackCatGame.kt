package io.itch.mattemade.blackcat

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.Game
import com.lehaine.littlekt.audio.AudioStream
import com.lehaine.littlekt.file.vfs.readBitmapFont
import com.lehaine.littlekt.graph.node.resource.HAlign
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.Textures
import com.lehaine.littlekt.graphics.g2d.SpriteBatch
import com.lehaine.littlekt.graphics.g2d.tilemap.tiled.TiledObjectLayer
import com.lehaine.littlekt.graphics.g2d.tilemap.tiled.TiledTilesLayer
import com.lehaine.littlekt.graphics.g2d.use
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.gl.TexMagFilter
import com.lehaine.littlekt.graphics.toFloatBits
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.util.milliseconds
import com.lehaine.littlekt.util.seconds
import com.lehaine.littlekt.util.viewport.FitViewport
import com.soywiz.korma.geom.Angle
import io.itch.mattemade.blackcat.assets.Assets
import io.itch.mattemade.blackcat.cat.Cat
import io.itch.mattemade.blackcat.input.GameInput
import io.itch.mattemade.blackcat.input.bindInputs
import io.itch.mattemade.blackcat.physics.Block
import io.itch.mattemade.blackcat.physics.ContactBits
import io.itch.mattemade.blackcat.physics.ContactListener
import io.itch.mattemade.blackcat.physics.Ladder
import io.itch.mattemade.blackcat.physics.Platform
import io.itch.mattemade.blackcat.physics.Wall
import io.itch.mattemade.blackcat.scene.GameScene
import io.itch.mattemade.utils.disposing.Disposing
import io.itch.mattemade.utils.disposing.Self
import io.itch.mattemade.utils.tiled.TiledBlockCombiner.combine
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.Filter
import org.jbox2d.dynamics.FixtureDef
import org.jbox2d.dynamics.World
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class BlackCatGame(
    context: Context,
    private val isSafari: Boolean
) : ContextListener(context), Disposing by Self() {

    private lateinit var gameScene: GameScene

    private val assets = Assets(context, ::handleAnimationSignal)
    private fun handleAnimationSignal(signal: String) {
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

    private val controller = context.bindInputs()

    override suspend fun Context.start() {

        val font = resourcesVfs["font/StoriaSans-Bold-120.fnt"].readBitmapFont(filter = TexMagFilter.LINEAR).disposing()
        val smallerFont =
            resourcesVfs["font/StoriaSans-Bold-60.fnt"].readBitmapFont(filter = TexMagFilter.LINEAR).disposing()

        gameScene = GameScene(context, controller, font, smallerFont, isSafari, assets, ::handleAnimationSignal)
        var lastWidth = 0
        var lastHeight = 0

        onResize { width, height ->
            lastWidth = width
            lastHeight = height
            with(gameScene) {
                resize(width, height)
            }
        }
        onRender { dt ->
            if (controller.pressed(GameInput.RESTART)) {
                gameScene.dispose()
                gameScene =  GameScene(context, controller, font, smallerFont, isSafari, assets, ::handleAnimationSignal)
                with(gameScene) {
                    resize(lastWidth, lastHeight)
                }
            }
            with(gameScene) {
                render(dt)
            }
        }

        onDispose {
            gameScene.dispose()
            dispose()
        }
    }

}
