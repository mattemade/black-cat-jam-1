package io.itch.mattemade.blackcat.assets

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.file.font.ttf.TtfFontReader
import com.lehaine.littlekt.file.vfs.readTiledMap
import com.lehaine.littlekt.file.vfs.readTtfFont

class Assets(context: Context, onAnimationSignal: (String) -> Unit): AssetPack(context) {

    val catAnimations = CatAnimations(context, onAnimationSignal).packed()
    val firstDay by prepare { context.resourcesVfs["maps/level1/level1.tmj"].readTiledMap() }
    val sounds = Sounds(context).packed()

}