package io.itch.mattemade.blackcat.assets

import com.lehaine.littlekt.Context

class CatAnimations(context: Context, onSignal: (String) -> Unit) : AssetPack(context, onSignal) {

    val idle by "texture/cat/idle".prepareAnimationPlayer()
    val walk by "texture/cat/walk".prepareAnimationPlayer()
    val crouch by "texture/cat/crouch".prepareAnimationPlayer()
    val crouchIdle by "texture/cat/crouchidle".prepareAnimationPlayer()
    val stand by "texture/cat/stand".prepareAnimationPlayer()
    val crawl by "texture/cat/crawl".prepareAnimationPlayer()
    val attack by "texture/cat/attack".prepareAnimationPlayer()
    val jump by "texture/cat/jump".prepareAnimationPlayer()
    val fall by "texture/cat/fall".prepareAnimationPlayer()
    val land by "texture/cat/land".prepareAnimationPlayer()
    val freefall by "texture/cat/freefall".prepareAnimationPlayer()
    val climbback by "texture/cat/climbingback".prepareAnimationPlayer()
    val climbwall by "texture/cat/climbingwall".prepareAnimationPlayer()

    val all by lazy { listOf(
        listOf(idle, walk, crouch, crawl),
        listOf(jump, fall, land, freefall),
        listOf(attack)
    ) }


}