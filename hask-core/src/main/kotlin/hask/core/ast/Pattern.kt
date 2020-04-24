/**
 *@author Nikolaus Knop
 */

package hask.core.ast

import hask.core.type.*

sealed class Pattern {
    abstract fun inferExpectedType(namer: Namer): Type

    abstract fun defineVars(namer: Namer): Env

    data class Integer(val value: Int) : Pattern() {
        override fun defineVars(namer: Namer): Env = emptyMap()

        override fun inferExpectedType(namer: Namer): Type = Type.INT

        override fun toString(): String = value.toString()
    }

    data class Constructor(val constructor: ADTConstructor, val names: List<String>) : Pattern() {
        private var typeArgs: MutableMap<String, Type> = mutableMapOf()

        override fun inferExpectedType(namer: Namer): Type {
            val typeParameters = constructor.adt.typeParameters
            val typeArguments = typeParameters.map { typeArgs.getOrPut(it) { Type.Var(namer.freshName()) } }
            return Type.ParameterizedADT(constructor.adt, typeArguments)
        }

        override fun defineVars(namer: Namer): Env = names.withIndex().associate { (idx, name) ->
            val type = constructor.parameters[idx]
            val actual = if (type is Type.Var) typeArgs.getOrPut(type.name) { Type.Var(namer.freshName()) } else type
            name to TypeScheme(emptySet(), actual)
        }

        override fun toString(): String = buildString {
            append(constructor.name)
            append(' ')
            names.joinTo(this, " ")
        }
    }

    object Otherwise: Pattern() {
        override fun inferExpectedType(namer: Namer): Type = Type.Var(namer.freshName())

        override fun defineVars(namer: Namer): Env = emptyMap()

        override fun toString(): String = "_"
    }
}