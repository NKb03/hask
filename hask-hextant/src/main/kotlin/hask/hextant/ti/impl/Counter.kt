/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti.impl

class Counter<T>(private val set: MutableSet<T> = mutableSetOf()) {
    private val counts = mutableMapOf<T, Int>()

    fun add(t: T): Boolean {
        val new = set.add(t)
        if (!new) counts[t] = counts[t]!! + 1
        else counts[t] = 1
        return new
    }

    fun remove(t: T): Boolean {
        return if (t in counts) {
            val old = counts[t]!!
            if (old == 1) {
                counts.remove(t)
                set.remove(t)
                true
            } else {
                counts[t] = old - 1
                false
            }
        } else false
    }

    fun addAll(ts: Iterable<T>) {
        ts.forEach { add(it) }
    }

    fun addAll(counter: Counter<T>) {
        counter.counts.forEach { (k, count) ->
            val new = set.add(k)
            if (!new) counts[k] = counts[k]!! + count
            else counts[k] = count
        }
    }

    fun removeAll(ts: Iterable<T>) {
        ts.forEach { remove(it) }
    }

    fun clear() {
        counts.clear()
        set.clear()
    }

    fun asSet(): Set<T> = set
}