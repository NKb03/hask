/**
 *@author Nikolaus Knop
 */

package hask.core.ast

import hask.core.type.*

sealed class Pattern {
    abstract fun inferExpectedType(namer: Namer, constructorADTMap: Map<ADTConstructor, ADT>): Type

    abstract fun defineVars(namer: Namer): Env

    open fun boundVariables(): Set<String> = emptySet()

    data class Integer(val value: Int) : Pattern() {
        override fun defineVars(namer: Namer): Env = emptyMap()

        override fun inferExpectedType(
            namer: Namer,
            constructorADTMap: Map<ADTConstructor, ADT>
        ): Type = Type.INT

        override fun toString(): String = value.toString()
    }

    data class Constructor(val constructor: ADTConstructor, val names: List<String>) : Pattern() {
        private var typeArgs: MutableMap<String, Type> = mutableMapOf()

        override fun inferExpectedType(namer: Namer, constructorADTMap: Map<ADTConstructor, ADT>): Type {
            val adt = constructorADTMap[constructor] ?: error("No adt found for constructor $constructor")
            val typeArguments = adt.typeParameters.map { typeArgs.getOrPut(it) { Type.Var(namer.freshName()) } }
            return Type.ParameterizedADT(adt.name, typeArguments)
        }

        override fun defineVars(namer: Namer): Env = names.withIndex().associate { (idx, name) ->
            val type = constructor.parameters[idx]
            val actual = if (type is Type.Var) typeArgs.getOrPut(type.name) { Type.Var(namer.freshName()) } else type
            name to TypeScheme(emptyList(), actual)
        }

        override fun boundVariables(): Set<String> = names.toSet()

        override fun toString(): String = buildString {
            append(constructor.name)
            append(' ')
            names.joinTo(this, " ")
        }
    }

    object Otherwise : Pattern() {
        override fun inferExpectedType(
            namer: Namer,
            constructorADTMap: Map<ADTConstructor, ADT>
        ): Type = Type.Var(namer.freshName())

        override fun defineVars(namer: Namer): Env = emptyMap()

        override fun toString(): String = "_"
    }
}