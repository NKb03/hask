/**
 * @author Nikolaus Knop
 */

package hask.core

import hask.core.ast.Expr
import hask.core.ast.Expr.*

fun lambda(vararg parameters: String, body: Expr) = Lambda(parameters.toList(), body)

fun apply(func: Expr, vararg args: Expr) = args.fold(func) { acc, arg -> Apply(acc, arg) }

fun apply(name: String, vararg args: Expr) = apply(ValueOf(name), *args)

val String.v get(): Expr = ValueOf(this)

val Int.l get(): Expr = IntLiteral(this)