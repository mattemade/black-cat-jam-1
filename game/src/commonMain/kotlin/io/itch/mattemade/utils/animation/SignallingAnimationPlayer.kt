package io.itch.mattemade.utils.animation

import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.g2d.AnimationPlayer
import kotlin.time.Duration

class SignallingAnimationPlayer(
    private val animationPlayerSequence: List<AnimationPlayerSpec>,
    private val animationPlayerFrames: List<Int>,
    private val signals: Map<Int, String>,
    private val callback: ((String) -> Unit)?
) {

    private var currentAnimationPlayerIndex = 0
    private val size = animationPlayerSequence.size
    private var totalFramesInPrevioselyPlayedAnimations = 0
    private var currentAnimationPlayer: AnimationPlayerSpec? = null
    val duration: Duration = animationPlayerSequence.map{ it.duration }.reduce { acc, duration -> acc + duration }

    init {
        updateCurrentPlayer()
    }

    private fun updateCurrentPlayer() {
        currentAnimationPlayer?.player?.apply {
            onFrameChange = null
        }
        currentAnimationPlayer = if (currentAnimationPlayerIndex < size) {
            if (currentAnimationPlayerIndex > 0) {
                totalFramesInPrevioselyPlayedAnimations += animationPlayerFrames[currentAnimationPlayerIndex - 1]
            }
            animationPlayerSequence[currentAnimationPlayerIndex].also {
                if (it.limitRepeats == 0) {
                    it.player.playLooped(it.animation)
                } else {
                    it.player.play(it.animation, times = it.limitRepeats)
                }
                it.player.onFrameChange = { frameIndex ->
                    signals[totalFramesInPrevioselyPlayedAnimations + frameIndex]?.let { signal -> callback?.invoke(signal) }
                }
                // restart changes frame to 0, and should be called after the onFrameChange callback is ready
                it.player.restart()
            }
        } else null
    }

    fun update(dt: Duration) {
        currentAnimationPlayer?.player?.update(dt)
        precalculatedCurrentKeyFrame = currentAnimationPlayer?.player?.currentKeyFrame
        while (currentAnimationPlayerIndex < size && precalculatedCurrentKeyFrame == null) {
            currentAnimationPlayerIndex++
            updateCurrentPlayer()
            precalculatedCurrentKeyFrame = currentAnimationPlayer?.player?.currentKeyFrame
        }
    }

    fun restart() {
        currentAnimationPlayerIndex = 0
        totalFramesInPrevioselyPlayedAnimations = 0
        updateCurrentPlayer()
    }

    private var precalculatedCurrentKeyFrame: Texture? = null
    val currentKeyFrame: Texture?
        get() = precalculatedCurrentKeyFrame


}