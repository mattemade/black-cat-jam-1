package io.itch.mattemade.blackcat

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.audio.AudioClip
import com.lehaine.littlekt.audio.AudioStream
import com.lehaine.littlekt.file.Vfs
import com.lehaine.littlekt.file.vfs.readBitmapFont
import com.lehaine.littlekt.graph.node.resource.HAlign
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.g2d.SpriteBatch
import com.lehaine.littlekt.graphics.g2d.tilemap.tiled.TiledObjectLayer
import com.lehaine.littlekt.graphics.g2d.tilemap.tiled.TiledTilesLayer
import com.lehaine.littlekt.graphics.g2d.use
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.gl.TexMagFilter
import com.lehaine.littlekt.graphics.toFloatBits
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.util.seconds
import com.lehaine.littlekt.util.viewport.FitViewport
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
import io.itch.mattemade.utils.disposing.Disposing
import io.itch.mattemade.utils.disposing.Self
import org.jbox2d.collision.shapes.CircleShape
import org.jbox2d.collision.shapes.MassData
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.Filter
import org.jbox2d.dynamics.FixtureDef
import org.jbox2d.dynamics.World
import org.jbox2d.dynamics.joints.DistanceJointDef
import org.jbox2d.dynamics.joints.RopeJointDef
import org.jbox2d.dynamics.joints.WeldJointDef
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class BlackCatGame(context: Context, private val safePlayClip: AudioClip.() -> Unit, private val safePlayStream: suspend AudioStream.(loop: Boolean) -> Unit) : ContextListener(context), Disposing by Self() {

    private val virtualWidth = 16
    private val virtualHeight = 9

    private val controller = context.bindInputs()


    private val batch = SpriteBatch(context).disposing()
    private val viewport = FitViewport(virtualWidth, virtualHeight)
    private val camera = viewport.camera
    private val preferredCameraPosition = Vec2()
    private val uiViewport = FitViewport(virtualWidth * 120, virtualHeight * 120)
    private val uiCamera = uiViewport.camera

    private val assets = Assets(context, ::handleAnimationSignal)

    private var animationResetTimer: Duration = 0.milliseconds
    private val timeLimit = 2f.seconds

    private var cameraOffsetX = 0f
    private var cameraOffsetY = 0f

    private val tempVec2 = Vec2()
    private val manualParallaxOrigin = Vec2()
    private val spawn by lazy {
        (assets.firstDay.layer("spawn") as? TiledObjectLayer)?.objects?.firstOrNull()?.let {
            Vec2(it.x, it.y).mulLocal(1 / 120f)
        } ?: error("")
    }
    private val cat by lazy {
            manualParallaxOrigin.set(spawn)
            Cat(spawn, world, assets.catAnimations, controller, ::handleAnimationSignal)
    }

    private val blocks = mutableListOf<Block>()
    private val walls = mutableListOf<Wall>()
    private val platforms = mutableListOf<Platform>()
    private val ladders = mutableListOf<Ladder>()

    private val world by lazy {
        World(gravity = Vec2(x = 0f, y = 40f)).registerAsContextDisposer(Body::class) {
            destroyBody(it as Body)
        }.apply {
            setContactListener(ContactListener())
            (assets.firstDay.layer("floor") as? TiledObjectLayer)?.objects?.forEach {
                blocks.add(
                    Block(
                        world,
                        it.bounds / 120f
                    ) {
                        categoryBits = ContactBits.BLOCK_BIT
                        maskBits = ContactBits.CAT_BIT
                    }
                )
            }
            (assets.firstDay.layer("block") as? TiledObjectLayer)?.objects?.forEach {
                blocks.add(
                    Block(
                        world,
                        it.bounds / 120f,
                        friction = 0f
                    ) {
                        categoryBits = ContactBits.BLOCK_BIT
                        maskBits = ContactBits.CAT_BIT
                    }
                )
            }
            (assets.firstDay.layer("wall") as? TiledObjectLayer)?.objects?.forEach {
                walls.add(
                    Wall(
                        world,
                        it.bounds / 120f,
                    ) {
                        categoryBits = ContactBits.WALL_BIT
                        maskBits = ContactBits.CAT_BIT
                    }
                )
            }
            (assets.firstDay.layer("platform") as? TiledObjectLayer)?.objects?.forEach {
                platforms.add(
                    Platform(
                        world,
                        it.bounds / 120f
                    )
                )
            }
            (assets.firstDay.layer("ladder") as? TiledObjectLayer)?.objects?.forEach {
                ladders.add(
                    Ladder(
                        world,
                        it.bounds / 120f
                    )
                )
            }
        }
    }

    private val manualParallaxOffset = mutableMapOf<String, Float>()
    private val backgroundLayers by lazy {
        assets.firstDay.layers.filterIsInstance<TiledTilesLayer>().filter {
            it.name.startsWith("bg_")
        }.also {
            it.forEach {
                manualParallaxOffset[it.name] = it.name.split("_")[1].toFloat() * 0.05f
            }
        }
    }
    private val foregroundLayers by lazy {
        assets.firstDay.layers.filterIsInstance<TiledTilesLayer>().filter {
            it.name.startsWith("fg_")
        }.also {
            it.forEach {
                manualParallaxOffset[it.name] = -it.name.split("_")[1].toFloat() * 0.05f
            }
        }
    }
    private val noCameraZones by lazy {
        (assets.firstDay.layer("nocamera") as? TiledObjectLayer)?.objects?.map {
            Block(world, it.bounds / 120f, friction = 0f, userData = "nocamera") {
                categoryBits = ContactBits.CAMERA_BIT
                maskBits = ContactBits.CAMERA_BIT
            }
        } ?: emptyList()
    }
    private val cameraBody by lazy {
        noCameraZones // initialize!!
        world.createBody(BodyDef(type = BodyType.DYNAMIC, gravityScale = 1f).apply {
            position.set(spawn.x, spawn.y - 2f)
        }).apply {
            createFixture(
                FixtureDef(
                    shape = PolygonShape().apply {
                        //radius = virtualHeight / 2f
                        setAsBox(virtualWidth / 2f, virtualHeight / 2f)
                     },
                    friction = 0f,
                    filter = Filter().apply {
                        categoryBits = ContactBits.CAMERA_BIT
                        maskBits = ContactBits.CAMERA_BIT
                    },
                    userData = "camera"
                )
            )
        }
    }

    private fun handleAnimationSignal(signal: String) {
        if (!touchedAtLeastOnce) {
            return
        }
        println(signal)
        when (signal) {
            "step" -> assets.sounds.nextStep
            "jump" -> assets.sounds.jump
            "land" -> assets.sounds.land
            "climb" -> assets.sounds.nextClimb
            "meow" -> assets.sounds.meow
            else -> null
        }?.safePlayClip()
    }

    private var touchedAtLeastOnce = false
    private var anyKeyPressed = true

    override suspend fun Context.start() {

        val font = resourcesVfs["font/StoriaSans-Bold-120.fnt"].readBitmapFont(filter = TexMagFilter.LINEAR).disposing()

        onResize { width, height ->
            viewport.update(width, height, this)
            uiViewport.update(width, height, this)
            touchedAtLeastOnce = false
        }
        onRender { dt ->
            if (!touchedAtLeastOnce) {
                assets.isLoaded // to initiate loading

                gl.clearColor(Color.BLACK)
                gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

                if (controller.justTouched || controller.isTouching || controller.pressed(GameInput.ANY)) {
                    touchedAtLeastOnce = true
                }

                uiViewport.apply(this, false)
                batch.use(uiCamera.viewProjection) {
                    font.draw(it, "Click here or\npress any button\nto start", 0f, -240f, align = HAlign.CENTER)
                }
                return@onRender
            }
            if (!assets.isLoaded) {

                gl.clearColor(Color.BLACK)
                gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

                uiViewport.apply(this, false)
                batch.use(uiCamera.viewProjection) {
                    font.draw(it, "Loading...", 0f, -60f, align = HAlign.CENTER)
                }
                return@onRender
            }

            if (!assets.bgMusic.playing) {
                context.vfs.launch {
                    assets.bgMusic.safePlayStream(true)
                    assets.forestAmbient.safePlayStream(true)
                }
            }

            gl.clearColor(Color.DARK_GRAY)
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            anyKeyPressed = anyKeyPressed || controller.pressed(GameInput.ANY)

            if (anyKeyPressed) {
                val isCatNearLadder = ladders.any { it.rect.intersects(cat.physicalRect) }
                val isCatOnLadder = ladders.any { it.rect.contains(cat.x, cat.y) }
                cat.update(dt, isCatNearLadder, isCatOnLadder)
                val catX = cat.directionX
                val catY = cat.directionY
                val cameraX = camera.position.x
                val cameraY = camera.position.y
                val horizontalBox = 0f
                val verticalBox = 0f
                var preferredCameraX = cameraX
                var preferredCameraY = cameraY

                if (catX > cameraX + horizontalBox) {
                    preferredCameraX = catX - horizontalBox
                } else if (catX < cameraX - horizontalBox) {
                    preferredCameraX = catX + horizontalBox
                }
                if (catY > cameraY + verticalBox) {
                    preferredCameraY = catY - verticalBox
                } else if (catY < cameraY - verticalBox) {
                    preferredCameraY = catY + verticalBox
                }
                tempVec2.set(preferredCameraX - cameraX, preferredCameraY - cameraY).mulLocal(tempVec2.length())
                cameraBody.linearVelocity.set(tempVec2)
                cameraBody.isAwake = true

                world.step(dt.seconds, 6, 2)
                camera.position.set(cameraBody.position.x, cameraBody.position.y, 0f)
                //camera.position.set(cat.x, cat.y, 0f)
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
                        val xOffset =
                            (camera.position.x - manualParallaxOrigin.x) * (manualParallaxOffset[it.name] ?: 0f)
                        it.render(batch, camera = camera, x = xOffset, y = 0f, scale = 1 / 120f)
                    }
                    cat.render(batch)
                    foregroundLayers.forEach {
                        val xOffset =
                            (camera.position.x - manualParallaxOrigin.x) * (manualParallaxOffset[it.name] ?: 0f)
                        it.render(batch, camera = camera, x = xOffset, y = 0f, scale = 1 / 120f)
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

private fun Rect.contains(x: Float, y: Float): Boolean =
    x >= this.x && x <= this.x2 && y >= this.y && y <= this.y2

private operator fun Rect.div(value: Float): Rect =
    Rect(x / value, y / value, width / value, height / value)
