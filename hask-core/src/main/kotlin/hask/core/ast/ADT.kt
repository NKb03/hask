/**
 *@author Nikolaus Knop
 */

package hask.core.ast

import hask.core.type.Type
import hask.core.type.Type.*

data class ADT(val name: String, val typeParameters: List<String>) {
    override fun toString(): String = buildString {
        append("data ")
        append(name)
        append(' ')
        typeParameters.joinTo(this, " ")
    }
}

data class ADTConstructor(val name: String, val parameters: List<Type>) {
    override fun toString(): String = buildString {
        append(name)
        for (p in parameters) {
            append(' ')
            if (p.isComplex()) append('(')
            append(p)
            if (p.isComplex()) append(')')
        }
    }
}

data class ADTDef(val adt: ADT, val constructors: List<ADTConstructor>) {
    override fun toString(): String = buildString {
        append(adt)
        append(" = ")
        constructors.joinTo(this, " | ")
    }
}

fun constructorFunc(adt: ADT, constructor: ADTConstructor): Builtin {
    val retType: Type = ParameterizedADT(adt.name, adt.typeParameters.map { Var(it) })
    val type = constructor.parameters.foldRight(retType) { param, ret -> Func(param, ret) }
    return Builtin(constructor.name, type)
}