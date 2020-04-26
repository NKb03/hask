/**
 *@author Nikolaus Knop
 */

package hask.core.ast

import hask.core.ast.Expr.*

fun lambda(vararg parameters: String, body: Expr) = parameters.foldRight(body) { p, acc -> Lambda(p, acc) }

fun apply(expr: Expr, vararg arguments: Expr) = arguments.fold(expr) { acc, a -> Apply(acc, a) }

fun apply(name: String, vararg arguments: Expr) = apply(ValueOf(name), *arguments)

infix fun String.be(value: Expr) = Binding(this, value)

fun let(vararg bindings: Binding, body: Expr) = Let(bindings.asList(), body)