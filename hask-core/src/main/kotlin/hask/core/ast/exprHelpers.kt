/**
 *@author Nikolaus Knop
 */

package hask.core.ast

import hask.core.ast.Expr.*

fun lambda(vararg parameters: String, body: Expr) = Lambda(parameters.asList(), body)

fun apply(function: Expr, vararg arguments: Expr) = Apply(function, arguments.toList())

fun apply(name: String, vararg arguments: Expr) = apply(ValueOf(name), *arguments)

infix fun String.be(value: Expr) = Binding(this, value)

fun let(vararg bindings: Binding, body: Expr) = Let(bindings.asList(), body)

val String.v get() = ValueOf(this)

val Int.l get() = IntLiteral(toString())