package io.itch.mattemade.blackcat.assets

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.audio.AudioClip
import com.lehaine.littlekt.file.vfs.readAudioClip

val SOUND_SUFFIX = ".mp3"

class Sounds(context: Context): AssetPack(context) {

    private val step1 by prepare { context.resourcesVfs["sound/Footsteps/step1.wav"].readAudioClip() }
    private val step2 by prepare { context.resourcesVfs["sound/Footsteps/step2.wav"].readAudioClip() }
    private val step3 by prepare { context.resourcesVfs["sound/Footsteps/step3.wav"].readAudioClip() }
    private val step4 by prepare { context.resourcesVfs["sound/Footsteps/step4.wav"].readAudioClip() }
    private val climb1 by prepare { context.resourcesVfs["sound/Climb/climb1.wav"].readAudioClip() }
    private val climb2 by prepare { context.resourcesVfs["sound/Climb/climb2.wav"].readAudioClip() }
    private val climb3 by prepare { context.resourcesVfs["sound/Climb/climb3.wav"].readAudioClip() }
    private val climb4 by prepare { context.resourcesVfs["sound/Climb/climb4.wav"].readAudioClip() }
    val jump by prepare { context.resourcesVfs["sound/Jump/Jump.wav"].readAudioClip() }
    val land by prepare { context.resourcesVfs["sound/Jump/Land.wav"].readAudioClip() }
    val meow by prepare { context.resourcesVfs["sound/meow$SOUND_SUFFIX"].readAudioClip() }


    private val steps by lazy{
        listOf(step1, step2, step3, step4)
    }
    private val climbs by lazy{
        listOf(climb1, climb2, climb3, climb4)
    }

    private var currentStep = 0
    val nextStep: AudioClip
        get() = steps[currentStep].also {
            currentStep = (currentStep + 1) % steps.size
        }

    private var currentClimb = 0
    val nextClimb: AudioClip
        get() = climbs[currentClimb].also {
            currentClimb = (currentClimb + 1) % climbs.size
        }
}