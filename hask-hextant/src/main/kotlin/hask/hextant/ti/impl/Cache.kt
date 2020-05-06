/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti.impl

import hask.hextant.ti.impl.Cache.State.Cached
import hask.hextant.ti.impl.Cache.State.Invalid

class Cache<T>(private val compute: () -> T) {
    private var state: State<T> = Invalid

    fun invalidate() {
        state = Invalid
    }

    fun get(): T = when (val s = state) {
        is Cached  -> s.value
        is Invalid -> compute().also { v -> state = Cached(v) }
    }

    private sealed class State<out T> {
        data class Cached<T>(val value: T) : State<T>()

        object Invalid : State<Nothing>()
    }
}