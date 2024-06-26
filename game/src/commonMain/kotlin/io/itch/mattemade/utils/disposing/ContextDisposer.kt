package io.itch.mattemade.utils.disposing

fun interface ContextDisposer {
    fun dispose(context: Any?)
}
