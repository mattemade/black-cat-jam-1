package io.itch.mattemade.blackcat

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.graph.node.resource.HAlign
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.Fonts
import com.lehaine.littlekt.graphics.g2d.SpriteBatch
import com.lehaine.littlekt.graphics.g2d.tilemap.tiled.TiledObjectLayer
import com.lehaine.littlekt.graphics.g2d.tilemap.tiled.TiledTilesLayer
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

    private val tempVec2 = Vec2()
    private val manualParallaxOrigin = Vec2()
    private val cat by lazy {
        (assets.firstDay.layer("spawn") as? TiledObjectLayer)?.objects?.firstOrNull()?.let { spawnPlace ->
            manualParallaxOrigin.set(
                spawnPlace.x,
                spawnPlace.y
            ).mulLocal(1 / 120f)
            Cat(manualParallaxOrigin, world, assets.catAnimations, controller)
        } ?: error("no spawn on the map!")
    }

    private val blocks = mutableListOf<Block>()
    private val platforms = mutableListOf<Platform>()
    private val ladders = mutableListOf<Ladder>()

    private val world by lazy {
        World(gravity = Vec2(x = 0f, y = 40f)).registerAsContextDisposer(Body::class) {
            destroyBody(it as Body)
        }.apply {
            setContactListener(ContactListener())
            (assets.firstDay.layer("floor") as? TiledObjectLayer)?.objects?.forEach { blocks.add(Block(world, it.bounds / 120f)) }
            (assets.firstDay.layer("wall") as? TiledObjectLayer)?.objects?.forEach { blocks.add(Block(world, it.bounds / 120f, friction = 0f)) }
            (assets.firstDay.layer("platform") as? TiledObjectLayer)?.objects?.forEach { platforms.add(Platform(world, it.bounds / 120f)) }
            (assets.firstDay.layer("ladder") as? TiledObjectLayer)?.objects?.forEach { ladders.add(Ladder(world, it.bounds / 120f)) }
        }
    }

    private val manualParallaxOffset = mutableMapOf<String, Float>()
    private val backgroundLayers by lazy {
        assets.firstDay.layers.filterIsInstance<TiledTilesLayer>().filter {
            it.name.startsWith("bg_")
        }.also { it.forEach {
            manualParallaxOffset[it.name] = it.name.split("_")[1].toFloat() * 0.05f
        } }
    }
    private val foregroundLayers by lazy {
        assets.firstDay.layers.filterIsInstance<TiledTilesLayer>().filter {
            it.name.startsWith("fg_")
        }.also { it.forEach {
            manualParallaxOffset[it.name] = -it.name.split("_")[1].toFloat() * 0.05f
        } }
    }

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
                val catX = cat.x
                val catY = cat.y
                val cameraX = camera.position.x
                val cameraY = camera.position.y
                val horizontalBox = 1.5f
                val verticalBox = 1.5f

                if (catX > cameraX + horizontalBox) {
                    camera.position.x = catX - horizontalBox
                } else if (catX < cameraX - horizontalBox) {
                    camera.position.x = catX + horizontalBox
                }
                if (catY > cameraY + verticalBox) {
                    camera.position.y = catY - verticalBox
                } else if (catY < cameraY - verticalBox) {
                    camera.position.y = catY + verticalBox
                }
            } else {
                animationResetTimer += dt
                if (animationResetTimer >= timeLimit) {
                    animationResetTimer -= timeLimit
                    assets.catAnimations.all.forEach { list -> list.forEach { it.restart() } }
                }
                assets.catAnimations.all.forEach { list -> list.forEach { it.update(dt) } }
            }

            viewport.apply(this)
            batch.use(camera.viewProjection) { batch ->
                if (anyKeyPressed) {
                    backgroundLayers.forEach {
                        val xOffset = (camera.position.x - manualParallaxOrigin.x)*manualParallaxOffset[it.name]!!
                        it.render(batch, camera = camera, x = xOffset, y = 0f, scale = 1/120f)
                    }
                    cat.render(batch)
                    foregroundLayers.forEach {
                        val xOffset = (camera.position.x - manualParallaxOrigin.x)*manualParallaxOffset[it.name]!!
                        it.render(batch, camera = camera, x = xOffset, y = 0f, scale = 1/120f)
                    }
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

private operator fun Rect.div(value: Float): Rect =
    Rect(x / value, y / value, width / value, height / value)
