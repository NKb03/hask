/**
 *@author Nikolaus Knop
 */

package hask.core.ast

import hask.core.parse.IDENTIFIER_REGEX
import hask.core.rt.NormalForm
import hask.core.type.Type

sealed class Expr {
    data class IntLiteral(val text: String) : Expr() {
        override fun toString(): String = text

        val num get() = text.toIntOrNull()
    }

    data class ValueOf(val name: String) : Expr() {
        override fun toString(): String = name
    }

    data class Lambda(val parameters: List<String>, val body: Expr) : Expr() {
        override fun toString(): String = "λ${parameters.joinToString(" ")} -> $body"
    }

    data class Apply(val function: Expr, val arguments: List<Expr>) : Expr() {
        override fun toString(): String = "($function ${arguments.joinToString(" ")})"
    }

    data class Binding(val name: String, val value: Expr)

    data class Let(val bindings: List<Binding>, val body: Expr) : Expr() {
        override fun toString(): String = "let ${bindings.joinToString(", ") { (n, v) -> "$n = $v" }} in $body"
    }

    data class If(val cond: Expr, val then: Expr, val otherwise: Expr) : Expr() {
        override fun toString(): String = "if $cond then $then else $otherwise"
    }

    data class ConstructorCall(val constructor: ADTConstructor, val arguments: List<Expr>) : Expr() {
        init {
            require(constructor.parameters.size == arguments.size)
        }

        override fun toString(): String = buildString {
            append(constructor.name)
            for (a in arguments) {
                append(' ')
                append(a)
            }
        }
    }

    data class Match(val expr: Expr, val arms: Map<Pattern, Expr>) : Expr() {
        override fun toString(): String = buildString {
            append("match ")
            append(expr)
            appendln(" with")
            for ((pattern, body) in arms) {
                append("  ")
                append(pattern)
                append(" -> ")
                appendln(body)
            }
        }
    }

    data class ApplyBuiltin(
        val name: String,
        val parameters: List<Type>,
        val returnType: Type,
        val arguments: List<Expr>,
        val function: (List<NormalForm>) -> NormalForm
    ) : Expr() {
        override fun toString(): String = buildString {
            append('(')
            append(name)
            for (a in arguments) {
                append(' ')
                append(a)
            }
            append(')')
        }
    }

    object Hole : Expr() {
        override fun toString(): String = "?"
    }

    fun containsHoles(): Boolean = when (this) {
        is IntLiteral      -> false
        is ValueOf         -> false
        is Lambda          -> body.containsHoles()
        is Apply           -> function.containsHoles() || arguments.any { it.containsHoles() }
        is Let             -> body.containsHoles() || bindings.any { it.value.containsHoles() }
        is If              -> cond.containsHoles() || then.containsHoles() || otherwise.containsHoles()
        is ConstructorCall -> arguments.any { it.containsHoles() }
        is Match           -> expr.containsHoles() || arms.values.any { it.containsHoles() }
        is ApplyBuiltin    -> arguments.any { it.containsHoles() }
        Hole               -> true
    }
}