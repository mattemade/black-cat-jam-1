package io.itch.mattemade.utils.disposing

interface HasContext<DisposingContext> {
    val context: Map<Any, DisposingContext>
}
