/**
 *@author Nikolaus Knop
 */

package hask.core.rt

import hask.core.ast.ADTConstructor
import hask.core.rt.Value.ADTValue
import hask.core.rt.Value.IntValue

sealed class Value {
    data class IntValue(val value: Int) : Value() {
        override fun toString(): String = value.toString()
    }

    data class ADTValue(val constructor: ADTConstructor, val fields: List<Thunk>) : Value() {
        override fun toString(): String = buildString {
            append(constructor.name)
            for (f in fields) {
                append(' ')
                append(f.force())
            }
        }
    }
}

fun Value.eq(other: Value) = when {
    this is IntValue && other is IntValue -> this.value == other.value
    this is ADTValue && other is ADTValue ->
        this.constructor == other.constructor &&
                this.fields.map { it.force() } == other.fields.map { it.force() }
    else -> false
}