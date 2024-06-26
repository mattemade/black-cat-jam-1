package io.itch.mattemade.utils.disposing

import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.GameAsset
import com.lehaine.littlekt.PreparableGameAsset

internal class Self : Disposing {
    private val disposables = mutableSetOf<Disposable>()
    private val contextDisposers = mutableMapOf<Any, (Any?) -> Unit>()

    override fun <T : Disposable> remember(block: () -> T): Lazy<T> =
        lazy { managed(block) }

    override fun <T : Disposable> T.disposing(): T =
        this.also { disposables += it }

    override fun <T : Disposable, U : GameAsset<T>> U.disposing(): AutoDisposingGameAsset<T> =
        AutoDisposingGameAsset(this).disposing()

    override fun <T : Disposable, U : PreparableGameAsset<T>> U.disposing(): AutoDisposingPreparableGameAsset<T> =
        AutoDisposingPreparableGameAsset(this).disposing()

    override fun <T : Disposable> managed(block: () -> T): T =
        block().disposing()

    override fun <K> K.registerAsContextDisposer(applicableTo: Any, block: K.(Any?) -> Unit): K {
        contextDisposers[applicableTo] = { this.block(it) }
        return this
    }

    override fun <T : Disposable> forget(disposable: T) {
        disposables.remove(disposable)
    }

    override fun dispose() {
        disposables.forEach {
            println("Disposing $it")
            if (it is HasContext<*>) {
                println("Disposing $it's context")
                it.context.forEach { (clazz, context) ->
                    contextDisposers[clazz]?.invoke(context)
                }
            }

            it.dispose()
        }
        disposables.clear()
    }
}
