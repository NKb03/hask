package hask.core.type

import org.omg.CORBA.DynAnyPackage.Invalid

sealed class Type {
    object INT : Type() {
        override fun toString(): String = "int"
    }

    data class Var(val name: String) : Type() {
        override fun toString(): String = name
    }

    data class Func(val from: Type, val to: Type) : Type() {
        override fun toString(): String = if (from.isComplex()) "($from) -> $to" else "$from -> $to"
    }

    data class ParameterizedADT(val adt: String, val typeArguments: List<Type>) : Type() {
        override fun toString(): String = buildString {
            append(adt)
            for (arg in typeArguments) {
                append(' ')
                val complex = arg.isComplex()
                if (complex) append('(')
                append(arg)
                if (complex) append(')')
            }
        }
    }

    object Hole : Type() {
        override fun toString(): String = "?"
    }

    fun apply(subst: Map<String, Type>): Type = when (this) {
        INT, Hole -> this
        is Var                -> subst[name] ?: this
        is Func               -> Func(from.apply(subst), to.apply(subst))
        is ParameterizedADT   -> ParameterizedADT(adt, typeArguments.map { it.apply(subst) })
    }

    fun subst(name: String, type: Type): Type = when (this) {
        INT, Hole -> this
        is Var                -> if (this.name == name) type else this
        is Func               -> Func(from.subst(name, type), to.subst(name, type))
        is ParameterizedADT   -> ParameterizedADT(adt, typeArguments.map { it.subst(name, type) })
    }

    fun fvs(env: Set<String> = emptySet()): Set<String> = when (this) {
        INT, Hole -> emptySet()
        is Var                -> if (name in env) emptySet() else setOf(name)
        is Func               -> from.fvs(env) + to.fvs(env)
        is ParameterizedADT   -> typeArguments.flatMapTo(mutableSetOf()) { it.fvs(env) }
    }

    fun isComplex(): Boolean = when (this) {
        INT, is Var, is Hole -> false
        is Func, is ParameterizedADT     -> true
    }

    fun generalize(env: Set<String>): TypeScheme {
        val parameters = fvs(env).toList()
        return TypeScheme(parameters, this)
    }
}