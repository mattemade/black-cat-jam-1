package io.itch.mattemade.utils.disposing

import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.GameAsset
import com.lehaine.littlekt.PreparableGameAsset

interface Disposing : Disposable {
    fun <T : Disposable> remember(block: () -> T): Lazy<T>
    fun <T : Disposable> T.disposing(): T
    fun <T : Disposable, U: GameAsset<T>> U.disposing(): AutoDisposingGameAsset<T>
    fun <T : Disposable, U: PreparableGameAsset<T>> U.disposing(): AutoDisposingPreparableGameAsset<T>
    fun <T : Disposable> managed(block: () -> T): T
    fun <K> K.registerAsContextDisposer(applicableTo: Any, block: K.(Any?) -> Unit): K
    fun <T: Disposable> forget(disposable: T)
}
