package io.itch.mattemade.blackcat.assets

import com.lehaine.littlekt.AssetProvider
import com.lehaine.littlekt.Context
import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.PreparableGameAsset
import io.itch.mattemade.utils.animation.SignallingAnimationPlayer
import io.itch.mattemade.utils.animation.readAnimationPlayer
import io.itch.mattemade.utils.disposing.Disposing
import io.itch.mattemade.utils.disposing.Self

open class AssetPack(protected val context: Context, private val defaultAnimationCallback: ((String) -> Unit)? = null) :
    Disposing by Self() {

    private val providers = mutableListOf<AssetProvider>()
    private val provider
        get() = AssetProvider(context).also { providers += it }

    private var providerWasFullyLoaded = false
    val isLoaded: Boolean
        get() =
            if (providerWasFullyLoaded) {
                true
            } else {
                var result = true
                providers.forEach {
                    if (!it.fullyLoaded) {
                        it.update()
                        result = false
                    }
                }
                providerWasFullyLoaded = result
                result
            }

    fun <T : Disposable> prepare(action: suspend () -> T): PreparableGameAsset<T> =
        provider.prepare { action().disposing() }

    protected fun String.prepareAnimationPlayer(callback: ((String) -> Unit)? = defaultAnimationCallback): PreparableGameAsset<SignallingAnimationPlayer> =
        provider.prepare { this.readAnimationPlayer(callback) }

    protected suspend fun String.readAnimationPlayer(callback: ((String) -> Unit)? = defaultAnimationCallback): SignallingAnimationPlayer =
        context.resourcesVfs[this].readAnimationPlayer(callback) { disposing() }

    fun <T : AssetPack> T.packed(): T {
        this@AssetPack.providers.addAll(providers)
        return this
    }

}

