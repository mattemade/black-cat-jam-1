package io.itch.mattemade.blackcat.assets

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.file.vfs.readAudioClip

class Sounds(context: Context): AssetPack(context) {

    private val step1 by prepare { context.resourcesVfs["sound/Footsteps/step1.wav"].readAudioClip() }
    private val step2 by prepare { context.resourcesVfs["sound/Footsteps/step2.wav"].readAudioClip() }
    private val step3 by prepare { context.resourcesVfs["sound/Footsteps/step3.wav"].readAudioClip() }
    private val step4 by prepare { context.resourcesVfs["sound/Footsteps/step4.wav"].readAudioClip() }
    private val climb1 by prepare { context.resourcesVfs["sound/Climb/climb1.wav"].readAudioClip() }
    private val climb2 by prepare { context.resourcesVfs["sound/Climb/climb2.wav"].readAudioClip() }
    private val climb3 by prepare { context.resourcesVfs["sound/Climb/climb3.wav"].readAudioClip() }
    private val climb4 by prepare { context.resourcesVfs["sound/Climb/climb4.wav"].readAudioClip() }
    private val death1 by prepare { context.resourcesVfs["sound/Death/Death1.wav"].readAudioClip() }
    private val death2 by prepare { context.resourcesVfs["sound/Death/Death2.wav"].readAudioClip() }
    private val death3 by prepare { context.resourcesVfs["sound/Death/Death3.wav"].readAudioClip() }
    val jump by prepare { context.resourcesVfs["sound/Jump/Jump.wav"].readAudioClip() }
    val land by prepare { context.resourcesVfs["sound/Jump/Land.wav"].readAudioClip() }
    val dash by prepare { context.resourcesVfs["sound/Jump/Dash.wav"].readAudioClip() }

    val step by lazy {
        SelectFrom(listOf(step1, step2, step3, step4))
    }

    val climb by lazy {
        SelectFrom(listOf(climb1, climb2, climb3, climb4))
    }

    val death by lazy {
        SelectFrom(listOf(death1, death2, death3))
    }


    class SelectFrom<T>(private val list: List<T>, private val fn: (Int) -> Int = { it + 1 }) {

        private val size = list.size
        private var nextIndex = 0

        val next: T
            get() = list[nextIndex].also {
                nextIndex = fn(nextIndex) % size
            }

    }
}