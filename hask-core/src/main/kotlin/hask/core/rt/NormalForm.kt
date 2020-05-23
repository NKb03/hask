/**
 *@author Nikolaus Knop
 */

package hask.core.rt

import hask.core.ast.ADTConstructor
import hask.core.ast.Expr
import hask.core.ast.Expr.*
import hask.core.type.freeVariables

sealed class NormalForm {
    data class IntValue(val value: Int) : NormalForm() {
        override fun toString(): String = value.toString()
    }

    data class ADTValue(val constructor: ADTConstructor, val fields: List<Thunk>) : NormalForm() {
        override fun toString(): String = buildString {
            append(constructor.name)
            for (f in fields) {
                append(' ')
                append(f.force())
            }
        }
    }

    data class Function(val parameters: List<String>, val body: Expr, val frame: StackFrame) : NormalForm() {
        override fun toString(): String = buildString {
            append("λ${parameters.joinToString(" ")} -> $body")
        }
    }

    data class Irreducible(val expr: Expr) : NormalForm() {
        override fun toString(): String = "$expr"
    }

    fun eq(other: NormalForm): Boolean = when {
        this is IntValue && other is IntValue -> this.value == other.value
        this is ADTValue && other is ADTValue ->
            this.constructor == other.constructor &&
                    this.fields.map { it.force() } == other.fields.map { it.force() }
        else                                  -> false
    }

    fun toExpr(env: Set<String>): Expr = when (this) {
        is IntValue    -> IntLiteral(value)
        is ADTValue    -> ConstructorCall(constructor, fields.map { it.force().toExpr(env) })
        is Function    -> {
            val bound = env + parameters
            val fvs = body.freeVariables(bound)
            val subst = fvs.associateWith { frame.getVar(it).force().toExpr(bound) }
            Lambda(parameters, body.substitute(subst))
        }
        is Irreducible -> expr
    }
}