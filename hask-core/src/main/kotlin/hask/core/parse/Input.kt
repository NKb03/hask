/**
 * @author Nikolaus Knop
 */

package hask.core.parse

interface Input<out T> {
    fun next(): T

    fun hasNext(): Boolean

    fun consume(): Input<T>

    fun location(): Location
}