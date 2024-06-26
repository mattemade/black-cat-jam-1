package io.itch.mattemade.utils.disposing

import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.PreparableGameAsset
import kotlin.reflect.KProperty

class AutoDisposingPreparableGameAsset<T : Disposable>(private val gameAsset: PreparableGameAsset<T>) : Disposable {

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