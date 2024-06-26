package io.itch.mattemade.utils.disposing

import com.lehaine.littlekt.Disposable

class MutableDisposingList<T : Disposable>(
    private val list: MutableList<T> = mutableListOf(),
    private val onDisposed: (T) -> Unit
) : MutableList<T> by list,
    Disposable {

    override fun add(element: T): Boolean {
        return list.add(element)
    }

    override fun clear() {
        list.forEach { it.also(onDisposed).dispose() }
        list.clear()
    }

    override fun removeAt(index: Int): T {
        return list.removeAt(index).also { it.also(onDisposed).dispose() }
    }

    override fun set(index: Int, element: T): T =
        list.set(index, element).also { it.also(onDisposed).dispose() }

    override fun retainAll(elements: Collection<T>): Boolean {
        TODO("Not yet implemented")
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        TODO("Not yet implemented")
    }

    override fun iterator(): MutableIterator<T> = IteratorWrapper(list.listIterator()) {
        it.also(onDisposed).dispose()
    }

    override fun listIterator(): MutableListIterator<T> = IteratorWrapper(list.listIterator()) {
        TODO("Not yet implemented")
    }

    override fun remove(element: T): Boolean {
        element.also(onDisposed).dispose()
        return list.remove(element)
    }

    override fun dispose() {
        clear()
    }

    private class IteratorWrapper<T : Disposable>(
        private val listIterator: MutableListIterator<T>,
        private val onRemoved: (T) -> Unit
    ) : MutableListIterator<T> by listIterator {

        private lateinit var current: T
        override fun next(): T =
            listIterator.next().also { current = it }

        override fun remove() {
            onRemoved(current)
            listIterator.remove()
        }

        override fun set(element: T) {
            onRemoved(current)
            listIterator.set(element)
        }
    }
}


inline fun <T : Disposable> mutableDisposableListOf(noinline onDisposed: (T) -> Unit): MutableDisposingList<T> =
    MutableDisposingList(onDisposed = onDisposed)
