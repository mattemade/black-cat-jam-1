package io.itch.mattemade.blackcat.scene

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.graph.node.resource.HAlign
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.g2d.SpriteBatch
import com.lehaine.littlekt.graphics.g2d.font.BitmapFont
import com.lehaine.littlekt.graphics.g2d.use
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.input.InputMapController
import com.lehaine.littlekt.util.viewport.FitViewport
import io.itch.mattemade.blackcat.assets.Assets
import io.itch.mattemade.blackcat.assets.Slides
import io.itch.mattemade.blackcat.input.GameInput
import io.itch.mattemade.utils.disposing.Disposing
import io.itch.mattemade.utils.disposing.Self
import kotlin.time.Duration

class SlideshowScene(
    private val context: Context,
    private val controller: InputMapController<GameInput>,
    private val gameAssets: Assets
): Disposing by Self() {

    var onFinish: () -> Unit = {}
    var smallerFont: BitmapFont? = null

    private val slides = Slides(context)
    private val batch = SpriteBatch(context).disposing()
    private val uiViewport = FitViewport(1920, 1080)
    private val uiCamera = uiViewport.camera

    private var currentTexture: Texture? = null

    fun Context.render(dt: Duration) {
        gl.clearColor(Color.BLACK)
        gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
        if (!slides.isLoaded) {
            return
        }

        if (controller.justTouched || controller.pressed(GameInput.ANY)) {
            currentTexture = slides.slides.next
        }

        if (currentTexture == null) {
            currentTexture = slides.slides.next
            if (currentTexture == null) {
                onFinish()
                return
            }
        }

        currentTexture?.let { texture: Texture ->
            uiViewport.apply(context)
            batch.use(uiCamera.viewProjection) { batch ->
                batch.draw(texture, 0f, 0f, width  = 1920f, height = 1080f)
                smallerFont?.draw(batch, "<click or press a button>", 960f, 1020f, align = HAlign.CENTER)
            }
        }

        if (!gameAssets.isLoaded) {
            // do nothing, just loading them in the background
        }
    }

    fun Context.resize(width: Int, height: Int) {
        uiViewport.update(width, height, context, centerCamera = true)
    }
}