package io.itch.mattemade.blackcat

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.graph.node.resource.HAlign
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.Fonts
import com.lehaine.littlekt.graphics.Textures
import com.lehaine.littlekt.graphics.g2d.SpriteBatch
import com.lehaine.littlekt.graphics.g2d.use
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.toFloatBits
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.util.seconds
import com.lehaine.littlekt.util.viewport.FitViewport
import io.itch.mattemade.blackcat.assets.Assets
import io.itch.mattemade.blackcat.cat.Cat
import io.itch.mattemade.blackcat.input.GameInput
import io.itch.mattemade.blackcat.input.bindInputs
import io.itch.mattemade.blackcat.physics.Block
import io.itch.mattemade.blackcat.physics.ContactListener
import io.itch.mattemade.blackcat.physics.Ladder
import io.itch.mattemade.blackcat.physics.Platform
import io.itch.mattemade.utils.disposing.Disposing
import io.itch.mattemade.utils.disposing.Self
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.World
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class BlackCatGame(context: Context) : ContextListener(context), Disposing by Self() {

    private val virtualWidth = 16
    private val virtualHeight = 9

    private val controller = context.bindInputs()


    private val batch = SpriteBatch(context).disposing()
    private val viewport = FitViewport(virtualWidth, virtualHeight)
    private val camera = viewport.camera

    private val assets = Assets(context)

    private var animationResetTimer: Duration = 0.milliseconds
    private val timeLimit = 2f.seconds

    private var cameraOffsetX = 0f
    private var cameraOffsetY = 0f

    private val cat by lazy { Cat(Vec2(-4f, -4f), world, assets.catAnimations, controller) }
    private val world = World(gravity = Vec2(x = 0f, y = 40f)).registerAsContextDisposer(Body::class) {
        destroyBody(it as Body)
    }.apply {
        setContactListener(ContactListener())
    }
    private val blocks = listOf(
        Block(world, Rect(-10f, 0f, 30f, 1f)),
        Block(world, Rect(-10f, -19f, 1f, 20f), friction = 0f),
        Block(world, Rect(20f, -19f, 1f, 20f), friction = 0f),
    )

    private val platformWidth = 3f
    private val platformGap = 1f
    private val period = platformWidth + platformGap
    private val diffList = listOf(0f, period, period * 2f, period * 3f, period * 2f, period, 0f, -period)
    private val platforms =
        List(12) { Platform(world, Rect(-1f + diffList[it % diffList.size], -1f + -1f * it, platformWidth, 1f)) }

    private val ladders =
        List(3) { Ladder(world, Rect(-6f + 8f*it, -10f, 1f, 8f)) }

    private var anyKeyPressed = true

    private val blockColor = Color.RED.toFloatBits()
    private val platformColor = Color.GREEN.toFloatBits()
    private val ladderColor = Color.BLUE.toFloatBits()

    override suspend fun Context.start() {
        onResize { width, height ->
            viewport.update(width, height, this)
        }
        onRender { dt ->
            if (!assets.isLoaded) {

                gl.clearColor(Color.BLACK)
                gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

                viewport.apply(this, false)
                batch.use(camera.viewProjection) {
                    Fonts.default.draw(it, "Loading...", 64f, -64f, align = HAlign.CENTER)
                }
                return@onRender
            }

            gl.clearColor(Color.DARK_GRAY)
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            anyKeyPressed = anyKeyPressed || controller.pressed(GameInput.ANY)

            if (anyKeyPressed) {
                val isCatNearLadder = ladders.any { it.rect.intersects(cat.physicalRect) }
                cat.update(dt, isCatNearLadder)
                world.step(dt.seconds, 6, 2)
                camera.position.x = cat.x//1920f/2f + (input.x - 1920f/2f)
                camera.position.y = cat.y//1080f * 1.5f + (input.y -1080f/2f)
            } else {
                animationResetTimer += dt
                if (animationResetTimer >= timeLimit) {
                    animationResetTimer -= timeLimit
                    assets.catAnimations.all.forEach { list -> list.forEach { it.restart() } }
                }
                assets.catAnimations.all.forEach { list -> list.forEach { it.update(dt) } }
            }

            viewport.apply(this)
            batch.use(camera.viewProjection) {
                assets.map.render(batch, camera, scale = 0.5f)
                if (anyKeyPressed) {
                    blocks.forEach { block ->
                        batch.draw(
                            Textures.white,
                            block.rect.x,
                            block.rect.y,
                            width = block.rect.width,
                            height = block.rect.height,
                            colorBits = blockColor
                        )
                    }

                    platforms.forEach { platform ->
                        batch.draw(
                            Textures.white,
                            platform.rect.x,
                            platform.rect.y,
                            width = platform.rect.width,
                            height = platform.rect.height,
                            colorBits = platformColor
                        )
                    }

                    ladders.forEach { ladder ->
                        batch.draw(
                            Textures.white,
                            ladder.rect.x,
                            ladder.rect.y,
                            width = ladder.rect.width,
                            height = ladder.rect.height,
                            colorBits = ladderColor
                        )
                    }
                    cat.render(it)
                } else {
                    var xOffset = -900f / 120f
                    var yOffset = -580f / 120f
                    assets.catAnimations.all.forEach { list ->
                        list.forEach { animation ->
                            animation.currentKeyFrame?.let {
                                batch.draw(
                                    it,
                                    xOffset,
                                    yOffset,
                                    width = 3.7f,
                                    height = 3.05f
                                )
                            }
                            xOffset += 450f / 120f
                        }
                        xOffset = -900f / 120f
                        yOffset += 370f / 120f
                    }
                }
            }
        }

        onDispose {
            dispose()
        }
    }
}