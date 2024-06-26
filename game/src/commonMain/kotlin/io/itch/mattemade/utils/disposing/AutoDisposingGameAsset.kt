package io.itch.mattemade.utils.disposing

import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.GameAsset
import kotlin.reflect.KProperty

class AutoDisposingGameAsset<T: Disposable>(private val gameAsset: GameAsset<T>): Disposable {

    private var content: T? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
        gameAsset.getValue(thisRef, property).also {
            val oldContent = content
            if (oldContent != it) {
                oldContent?.dispose()
                content = it
            }
        }

    override fun dispose() {
        content?.dispose()
    }
}