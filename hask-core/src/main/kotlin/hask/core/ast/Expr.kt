/**
 *@author Nikolaus Knop
 */

package hask.core.ast

import hask.core.rt.Value
import hask.core.type.Type

sealed class Expr {
    data class IntLiteral(val num: Int) : Expr() {
        override fun toString(): String = "$num"
    }

    data class ValueOf(val name: String) : Expr() {
        override fun toString(): String = name
    }

    data class Lambda(val parameter: String, val body: Expr) : Expr() {
        override fun toString(): String = "λ$parameter -> $body"
    }

    data class Apply(val l: Expr, val r: Expr) : Expr() {
        override fun toString(): String = "($l $r)"
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
        val function: (List<Value>) -> Value
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
}