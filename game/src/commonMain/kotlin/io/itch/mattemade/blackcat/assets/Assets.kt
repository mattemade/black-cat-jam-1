package io.itch.mattemade.blackcat.assets

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.file.vfs.readTiledMap

class Assets(context: Context): AssetPack(context) {

    val catAnimations = CatAnimations(context).packed()
    val map by prepare { context.resourcesVfs["maps/test-env/test-env.tmj"].readTiledMap() }

}