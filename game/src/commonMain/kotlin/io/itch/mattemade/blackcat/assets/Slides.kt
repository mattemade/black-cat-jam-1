package io.itch.mattemade.blackcat.assets

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.file.vfs.readTexture

class Slides(context: Context) : AssetPack(context, {}) {

    private val slide1 by prepare { context.resourcesVfs["texture/slides/slide.1.png"].readTexture() }
    private val slide2 by prepare { context.resourcesVfs["texture/slides/slide.2.png"].readTexture() }
    private val slide3 by prepare { context.resourcesVfs["texture/slides/slide.3.png"].readTexture() }
    private val slide4 by prepare { context.resourcesVfs["texture/slides/slide.4.png"].readTexture() }
    private val slide5 by prepare { context.resourcesVfs["texture/slides/slide.5.png"].readTexture() }
    private val slide6 by prepare { context.resourcesVfs["texture/slides/slide.6.png"].readTexture() }

    val slides by lazy {
        SelectFrom(listOf(slide1, slide2, slide3, slide4, slide5, slide6))
    }

    class SelectFrom<T>(private val list: List<T>, private val fn: (Int) -> Int = { it + 1 }) {

        private val size = list.size
        private var nextIndex = 0

        val next: T?
            get() = list.getOrNull(nextIndex++)

    }
}