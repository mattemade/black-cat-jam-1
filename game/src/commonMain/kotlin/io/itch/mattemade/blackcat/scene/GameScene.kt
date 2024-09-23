package io.itch.mattemade.blackcat.scene

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.Scene
import com.lehaine.littlekt.audio.AudioStream
import com.lehaine.littlekt.file.vfs.readBitmapFont
import com.lehaine.littlekt.graph.node.resource.HAlign
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.Textures
import com.lehaine.littlekt.graphics.g2d.SpriteBatch
import com.lehaine.littlekt.graphics.g2d.font.BitmapFont
import com.lehaine.littlekt.graphics.g2d.tilemap.tiled.TiledObjectLayer
import com.lehaine.littlekt.graphics.g2d.tilemap.tiled.TiledTilesLayer
import com.lehaine.littlekt.graphics.g2d.use
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.gl.TexMagFilter
import com.lehaine.littlekt.graphics.toFloatBits
import com.lehaine.littlekt.input.InputMapController
import com.lehaine.littlekt.math.Rect
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

class GameScene(private val context: Context, private val controller: InputMapController<GameInput>, private val font: BitmapFont, private val assets: Assets, private val initialIgnoreFirstResize: Boolean, private val handleAnimationSignals: (String) -> Unit): Disposing by Self() {


    private val virtualWidth = 16
    private val virtualHeight = 9



    private val batch = SpriteBatch(context).disposing()
    private val viewport = FitViewport(virtualWidth, virtualHeight)
    private val camera = viewport.camera
    private val preferredCameraPosition = Vec2()
    private val uiViewport = FitViewport(virtualWidth * 120, virtualHeight * 120)
    private val uiCamera = uiViewport.camera


    private var animationResetTimer: Duration = 0.milliseconds
    private val timeLimit = 2f.seconds
    private var internalTimer: Duration = Duration.ZERO

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
        Cat(spawn, world, assets.catAnimations, controller, {
            if (touchedAtLeastOnce) {
                handleAnimationSignals(it)
            }
        })
    }

    private val blocks = mutableListOf<Block>()
    private val walls = mutableListOf<Wall>()
    private val platforms = mutableListOf<Platform>()
    private val ladders = mutableListOf<Ladder>()
    private val cameraZones = mutableListOf<Rect>()
    private val dangerZones = mutableListOf<Rect>()
    private val finish = Rect()

    private var nearCaveEntrance: Boolean = false
    private var caveRects = mutableListOf<Rect>()
    private val disabledLayers = mutableSetOf<String>().apply {
        /*add("fg_0_grass")
        add("fg_0_cave")
        add("bg_0_ground")*/
    }



    init {
        context.storageVfs["store"].readKeystore()?.split("\n")?.forEachIndexed { index, line ->
            when (index) {
                0 -> {
                    bestTime = line.toLong()
                    bestTimeText = formattedTime(bestTime)
                }
                1 -> name = line
            }
        }
    }


    private val world by lazy {
        World(gravity = Vec2(x = 0f, y = 40f)).registerAsContextDisposer(Body::class) {
            destroyBody(it as Body)
        }.apply {
            setContactListener(ContactListener())
            (assets.firstDay.layer("camera") as? TiledObjectLayer)?.objects?.forEach {
                cameraZones.add(it.bounds / 120f)
            }
            (assets.firstDay.layer("danger") as? TiledObjectLayer)?.objects?.forEach {
                dangerZones.add(it.bounds / 120f)
            }
            (assets.firstDay.layer("cave") as? TiledObjectLayer)?.objects?.forEach {
                caveRects.add(it.bounds / 120f)
            }
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
            assets.firstDay.combine { name, rect ->
                when (name) {
                    "block" -> blocks.add(
                        Block(
                            world,
                            rect,
                            friction = 0f
                        ) {
                            categoryBits = ContactBits.BLOCK_BIT
                            maskBits = ContactBits.CAT_BIT
                        }
                    )

                    "wall" -> walls.add(
                        Wall(
                            world,
                            rect,
                        ) {
                            categoryBits = ContactBits.WALL_BIT
                            maskBits = ContactBits.CAT_BIT
                        }
                    )

                    "death" -> blocks.add(
                        Block(
                            world,
                            rect,
                            userData = "death"
                        ) {
                            categoryBits = ContactBits.WALL_BIT
                            maskBits = ContactBits.CAT_BIT
                        }
                    )

                    "ladder" -> ladders.add(
                        Ladder(
                            world,
                            rect
                        )
                    )

                    "platform" -> platforms.add(
                        Platform(
                            world,
                            rect
                        )
                    )

                    "ladderplatform" -> {
                        ladders.add(
                            Ladder(
                                world,
                                rect
                            )
                        )
                        platforms.add(
                            Platform(
                                world,
                                rect
                            )
                        )
                    }
                }
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
            assets.firstDay.layers.forEach { it.visible = true }

            (assets.firstDay.layer("finish") as? TiledObjectLayer)?.objects?.firstOrNull()?.let {
                val rect = it.bounds / 120f
                finish.set(rect.x, rect.y, rect.width, rect.height)
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


    private val ambients by lazy {
        listOf(
            assets.forestAmbient,
            assets.caveAmbient,
            assets.mountainAmbient,
        )
    }
    private var currentAmbient = 0
    private val ambientVolume = Array(3) { if (it == currentAmbient) 1f else 0f }
    private var musicTargetVolume = 1f
        set(value) {
            field = value
        }

    private var touchedAtLeastOnce = true // default true since we had some touches in slideshow
    private var anyKeyPressed = true
    private var gameFinished = false
    private var bestTime = 0L
    private var bestTimeText = ""
    private var name = "AAAAA"


    suspend fun Context.show() {

    }

    private var ignoreFirstResize = initialIgnoreFirstResize

    fun Context.resize(width: Int, height: Int) {
        viewport.update(width, height, this)
        uiViewport.update(width, height, this)
        if (ignoreFirstResize) {
            ignoreFirstResize = false
        } else {
            touchedAtLeastOnce = false
        }
    }

    fun Context.render(dt: Duration) {
        if (!touchedAtLeastOnce) {
            assets.isLoaded // to initiate loading

            gl.clearColor(Color.BLACK)
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            if (controller.justTouched || controller.pressed(GameInput.ANY)) {
                touchedAtLeastOnce = true
            }

            uiViewport.apply(this, false)
            batch.use(uiCamera.viewProjection) {
                font.draw(it, "Click here or\npress any button\nto resume", 0f, -240f, align = HAlign.CENTER)
            }
            return
        }
        if (!assets.isLoaded) {

            gl.clearColor(Color.BLACK)
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            uiViewport.apply(this, false)
            batch.use(uiCamera.viewProjection) {
                font.draw(it, "Loading...", 0f, -60f, align = HAlign.CENTER)
            }
            return
        }

        if (!assets.bgMusic.playing) {
            context.vfs.launch {
                assets.bgMusic.play(loop = true, volume = musicTargetVolume)
                ambients.forEachIndexed { index, it ->
                    it.play(loop = true, volume = ambientVolume[index])
                }
            }
        }

        assets.bgMusic.adjustVolume(musicTargetVolume, dt, speedingFactor = 0.5f)
        ambients.forEachIndexed { index, it ->
            it.adjustVolume(ambientVolume[index], dt)
        }

        gl.clearColor(Color.BLACK)
        gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

        anyKeyPressed = anyKeyPressed || controller.pressed(GameInput.ANY)

        if (anyKeyPressed) {
            val isCatNearLadder = ladders.any { it.rect.intersects(cat.physicalRect) }
            val isCatOnLadder = ladders.any { it.rect.contains(cat.x, cat.y) }
            val isDanger = dangerZones.any { it.contains(cat.x, cat.y) }
            cat.update(dt, isCatNearLadder, isCatOnLadder, isDanger)
            updateCameraBodyPosition()

            world.step(dt.seconds, 6, 2)

            if (!gameFinished) {
                if (finish.contains(cat.x, cat.y)) {
                    gameFinished = true
                    val bestTime = if (bestTime == 0L) internalTimer.inWholeMilliseconds else min(
                        bestTime,
                        internalTimer.inWholeMilliseconds
                    )
                    //bestTimeText = formattedTime(bestTime)
                    vfs.launch {
                        storageVfs["store"].writeKeystore("$bestTime\n$name")
                    }
                } else {
                    internalTimer += dt
                }
            }

            camera.position.set(cameraBody.position.x, cameraBody.position.y, 0f)

            if (nearCaveEntrance) {
                if (caveRects.all { cat.y > it.y2 || cat.y < it.y || cat.x > it.x2 }) { // forest
                    musicTargetVolume = min(1f, max(0f, (cat.y - finish.y) / (spawn.y - finish.y)))
                    nearCaveEntrance = false
                    changeAmbient(0)
                    disabledLayers.clear()
                    disabledLayers.add("bg_0_cave_walls")
                    disabledLayers.add("bg_0_cave_bg")
                } else if (caveRects.all { cat.y > it.y2 || cat.y < it.y || cat.x < it.x }) { // cave
                    musicTargetVolume = 0f
                    nearCaveEntrance = false
                    changeAmbient(1)
                    disabledLayers.clear()
                    disabledLayers.add("fg_0_grass")
                    disabledLayers.add("fg_0_cave")
                    disabledLayers.add("bg_0_ground")
                } else {
                    musicTargetVolume = min(1f, max(0f, (cat.y - finish.y) / (spawn.y - finish.y)))
                    //musicTargetVolume = (cat.x - caveRect.x) / (caveRect.width)
                }
            } else if (caveRects.any { it.contains(cat.x, cat.y) }) {
                musicTargetVolume = min(1f, max(0f, (cat.y - finish.y) / (spawn.y - finish.y)))
                nearCaveEntrance = true
                disabledLayers.clear()
                //disabledLayers.add("fg_0_grass")
            } else if (disabledLayers.isEmpty() || disabledLayers.size == 2) { // terrible way to check if we are outside
                val finishRatio = min(1f, max(0f, (cat.y - finish.y) / (spawn.y - finish.y)))
                musicTargetVolume = finishRatio*finishRatio
                ambientVolume[0] = finishRatio
                ambientVolume[2] = 1f - finishRatio
            }
            //camera.position.set(cat.x, cat.y, 0f)
        } else {
            animationResetTimer += dt
            if (animationResetTimer >= timeLimit) {
                animationResetTimer -= timeLimit
                assets.catAnimations.all.forEach { list -> list.forEach { it.restart() } }
            }
            assets.catAnimations.all.forEach { list -> list.forEach { it.update(dt) } }
        }

        uiViewport.apply(this)
        batch.use(uiCamera.viewProjection) { batch ->
            batch.draw(
                Textures.white,
                -1000f,
                -540f,
                width = 2000f,
                height = 1080f,
                colorBits = Color.LIGHT_BLUE.toFloatBits()
            )
        }

        viewport.apply(this)
        batch.use(camera.viewProjection) { batch ->
            if (anyKeyPressed) {
                backgroundLayers.forEach {
                    if (!disabledLayers.contains(it.name)) {
                        val xOffset =
                            (camera.position.x - manualParallaxOrigin.x) * (manualParallaxOffset[it.name] ?: 0f)
                        it.render(batch, camera = camera, x = xOffset, y = 0f, scale = 1 / 120f)
                    }
                }
                cat.render(batch)
                foregroundLayers.forEach {
                    if (!disabledLayers.contains(it.name)) {
                        val xOffset =
                            (camera.position.x - manualParallaxOrigin.x) * (manualParallaxOffset[it.name] ?: 0f)
                        it.render(batch, camera = camera, x = xOffset, y = 0f, scale = 1 / 120f)
                    }
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

        uiViewport.apply(this)
        batch.use(uiCamera.viewProjection) { batch ->
            if (bestTime != 0L) {
                if (bestTimeText.length == 0) {
                    bestTimeText = formattedTime(bestTime)
                }
                val formatted = formattedTime(internalTimer.inWholeMilliseconds)
                assets.monoFont.draw(batch, "$formatted / $bestTimeText", x = -950f, y = -540f)
            }
            if (gameFinished) {
                val formatted = formattedTime(internalTimer.inWholeMilliseconds)
                font.draw(batch, "Congratulations!\nYou've finished the game in $formatted\nPress R to restart the game", 0f, -240f, align = HAlign.CENTER)
            }
        }
    }


    private fun formattedTime(totalMilliseconds: Long): String {
        //val totalMilliseconds = internalTimer.inWholeMilliseconds
        val millis = totalMilliseconds % 1000L
        val seconds = (totalMilliseconds / 1000L) % 60
        val minutes = (totalMilliseconds / 1000L) / 60
        val formatted =
            "${if (minutes < 10) "0$minutes" else minutes}:${if (seconds < 10) "0$seconds" else seconds}.${if (millis < 10) "00$millis" else if (millis < 100) "0$millis" else millis}"
        return formatted
    }

    private fun updateCameraBodyPosition() {
        cameraZones.firstOrNull {
            it.contains(cat.x, cat.y)
        }?.let { zone ->
            viewport.virtualWidth = zone.width
            viewport.virtualHeight = zone.height
            cameraBody.setTransform(tempVec2.set((zone.x + zone.x2) / 2f, (zone.y + zone.y2) / 2f), Angle.ZERO)
            cameraBody.isAwake = false
            return
        } ?: run {
            viewport.virtualWidth = virtualWidth.toFloat()
            viewport.virtualHeight = virtualHeight.toFloat()
        }
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
    }

    private fun changeAmbient(index: Int) {
        ambientVolume[currentAmbient] = 0f
        currentAmbient = index
        ambientVolume[currentAmbient] = 1f
    }
}


fun AudioStream.adjustVolume(target: Float, dt: Duration, speedingFactor: Float = 3f) {
    if (volume > target) {
        volume = max(0f, volume - dt.seconds / speedingFactor)
    } else if (volume < target) {
        volume = min(1f, volume + dt.seconds / speedingFactor)
    }
}

private fun Rect.contains(x: Float, y: Float): Boolean =
    x >= this.x && x <= this.x2 && y >= this.y && y <= this.y2

private operator fun Rect.div(value: Float): Rect =
    Rect(x / value, y / value, width / value, height / value)
