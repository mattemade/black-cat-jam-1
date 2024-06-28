package io.itch.mattemade.blackcat.assets

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.audio.AudioClip
import com.lehaine.littlekt.file.vfs.readAudioClip

class Sounds(context: Context): AssetPack(context) {

    private val step1 by prepare { context.resourcesVfs["sound/step1.mp3"].readAudioClip() }
    private val step2 by prepare { context.resourcesVfs["sound/step2.mp3"].readAudioClip() }
    private val climb1 by prepare { context.resourcesVfs["sound/climb1.mp3"].readAudioClip() }
    private val climb2 by prepare { context.resourcesVfs["sound/climb2.mp3"].readAudioClip() }
    val jump by prepare { context.resourcesVfs["sound/jump.mp3"].readAudioClip() }
    val land by prepare { context.resourcesVfs["sound/land.mp3"].readAudioClip() }
    val attack by prepare { context.resourcesVfs["sound/attack.mp3"].readAudioClip() }


    private val steps by lazy{
        listOf(step1, step2)
    }
    private val climbs by lazy{
        listOf(climb1, climb2)
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