package io.itch.mattemade.utils.animation

import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.file.vfs.VfsFile
import com.lehaine.littlekt.file.vfs.readTexture
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.g2d.Animation
import com.lehaine.littlekt.graphics.g2d.AnimationPlayer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class AnimationSpec(
    val path: String,
    private val formatPrefix: String,
    private val formatSuffix: String,
    private val formatNumberDigits: Int,
    val framesCount: Int,
    val frameSpecs: List<AnimationFrameSpec>,
    val signals: Map<Int, String>
) {
    fun getFramePath(frame: Int): String {
        val frameString = frame.toString()
        val frameLength = frameString.length
        if (frameLength > formatNumberDigits) {
            return "$path/$formatPrefix$frameString$formatSuffix"
        } else {
            var trailingZeros = ""
            for (i in 1..(formatNumberDigits - frameLength)) {
                trailingZeros += "0"
            }
            return "$path/$formatPrefix$trailingZeros$frameString$formatSuffix"
        }
    }
}

data class AnimationFrameSpec(
    val frameIndicies: List<Int>,
    val frameDuration: List<Duration>,
    val repeatLogic: String,
)

data class AnimationPlayerSpec(
    val player: AnimationPlayer<Texture>,
    val animation: Animation<Texture>,
    val limitRepeats: Int = 0,
    val duration: Duration,
)

suspend fun VfsFile.readAnimationPlayer(
    signalCallback: ((String) -> Unit)? = null,
    registerDisposable: Disposable.() -> Unit
): SignallingAnimationPlayer =
    readAnimationSpec().let { spec ->
        val frames = (1..spec.framesCount).map { vfs[spec.getFramePath(it)].readTexture().also(registerDisposable) }
        val framesPerPlayer = mutableListOf<Int>()
        val players = spec.frameSpecs.map { frameSpec ->
            /*val animation = Animation(
                frames = (0..<frameSpec.frameIndicies.size).map { frames[frameSpec.frameIndicies[it]] },
                frameIndices = frameSpec.frameIndicies.indices.toList(),
                frameTimes = frameSpec.frameDuration,
            )*/
            framesPerPlayer += frameSpec.frameIndicies.size
            val limitRepeats = if (frameSpec.repeatLogic.isBlank()) 0 else frameSpec.repeatLogic.toInt()
            val totalFrameDurations = frameSpec.frameDuration.reduce { acc, duration -> acc + duration }
            AnimationPlayerSpec(
                player = AnimationPlayer<Texture>()/*.apply {
                    when (frameSpec.repeatLogic) {
                        "" -> playLooped(animation)
                        else -> play(animation, times = frameSpec.repeatLogic.toInt())
                    }
                }*/,
                animation = Animation(
                    frames = (0..<frameSpec.frameIndicies.size).map { frames[frameSpec.frameIndicies[it]] },
                    frameIndices = frameSpec.frameIndicies.indices.toList(),
                    frameTimes = frameSpec.frameDuration,
                ),
                limitRepeats = limitRepeats,
                duration = totalFrameDurations * limitRepeats,
            )

        }

        SignallingAnimationPlayer(players, framesPerPlayer, spec.signals, signalCallback)
    }

suspend fun VfsFile.readAnimationSpec(): AnimationSpec {
    val specText = get("spec.txt").readLines().map { it.trim() }

    var frameIndices = mutableListOf<Int>()
    var frameTimes = mutableListOf<Duration>()
    val signals = mutableMapOf<Int, String>()
    var repeatLogic = "1"
    var framesCount = 0
    var format = ""
    val frameSpecs = mutableListOf<AnimationFrameSpec>()
    var framesPerAllPreviousAnimations = 0
    specText.forEach { line ->
        if (!line.startsWith("#")) {
            val formatSplit = line.split(" frames like ")
            val sequenceSplit = line.split(" for ")
            if (formatSplit.size == 2) {
                framesCount = formatSplit[0].toInt()
                format = formatSplit[1]
            } else if (sequenceSplit.size == 2) {
                val rangeSplit = sequenceSplit[0].split("..")
                val frameTime = sequenceSplit[1].toInt().milliseconds
                if (rangeSplit.size == 2) {
                    val start = rangeSplit[0].toInt()
                    val end = rangeSplit[1].toInt()
                    val range = if (start < end) start..end else start downTo end
                    for (frame in range) {
                        frameIndices += frame - 1
                        frameTimes += frameTime
                    }
                } else {
                    frameIndices += sequenceSplit[0].toInt() - 1
                    frameTimes += frameTime
                }
            } else if (line.startsWith("repeat")) {
                repeatLogic = if (line.length > 7) line.substring(7) else ""
                frameSpecs += AnimationFrameSpec(
                    frameIndices,
                    frameTimes,
                    repeatLogic,
                )
                framesPerAllPreviousAnimations += frameIndices.size
                frameIndices = mutableListOf<Int>()
                frameTimes = mutableListOf<Duration>()
                repeatLogic = ""
            } else if (line.startsWith("signal")) {
                val signal = if (line.length > 7) line.substring(7) else ""
                signals[framesPerAllPreviousAnimations + frameIndices.size] = signal
            }
        }
    }
    val formatSplit = format.split(Regex("%+"))

    return AnimationSpec(
        path,
        formatPrefix = formatSplit[0],
        formatSuffix = formatSplit[1],
        format.count { it == '%' },
        framesCount,
        frameSpecs,
        signals
    )
}
/*
7 frames like walk.%%%%.png
1..7 for 16
repeat from start
*/