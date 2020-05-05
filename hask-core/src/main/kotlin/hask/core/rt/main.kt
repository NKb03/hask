/**
 * @author Nikolaus Knop
 */

package hask.core.rt

import hask.core.ast.*
import hask.core.ast.Expr.*
import hask.core.parse.*
import hask.core.rt.NormalForm.IntValue
import hask.core.type.Type
import hask.core.type.Type.ParameterizedADT
import hask.core.type.inferType

operator fun Expr.minus(r: Expr) = ApplyBuiltin("-", listOf(Type.INT, Type.INT), Type.INT, listOf(this, r)) { (l, r) ->
    IntValue((l as IntValue).value - (r as IntValue).value)
}

operator fun Expr.times(r: Expr) = ApplyBuiltin("*", listOf(Type.INT, Type.INT), Type.INT, listOf(this, r)) { (l, r) ->
    IntValue((l as IntValue).value * (r as IntValue).value)
}

operator fun Expr.plus(r: Expr) = ApplyBuiltin("*", listOf(Type.INT, Type.INT), Type.INT, listOf(this, r)) { (l, r) ->
    IntValue((l as IntValue).value + (r as IntValue).value)
}

operator fun Expr.invoke(vararg expressions: Expr): Expr = apply(this, *expressions)

operator fun String.invoke(vararg expressions: Expr): Expr = ValueOf(this).invoke(*expressions)

val fac = lambda(
    "n",
    body = Match(
        "n".v, mapOf(
            Pattern.Integer(0) to 1.l,
            Pattern.Otherwise to "n".v * "fac"("n".v - 1.l)
        )
    )
)

val List = ADT("List", listOf("a"))

val Empty = ADTConstructor(List, "Empty", emptyList())

val Cons = ADTConstructor(List, "Cons", listOf(Type.Var("a"), ParameterizedADT(List, listOf(Type.Var("a")))))

infix fun Expr.cons(rest: Expr) = ConstructorCall(Cons, listOf(this, rest))

fun empty() = ConstructorCall(Empty, emptyList())

val map = lambda(
    "f", "list", body = Match(
        "list".v, mapOf(
            Pattern.Constructor(Empty, emptyList()) to empty(),
            Pattern.Constructor(Cons, listOf("x", "xs")) to ("f"("x".v) cons "map"("f".v, "xs".v))
        )
    )
)

fun main() {
    tryMap()
    tryFac()
    val e = let("a" be "b".v + 2.l, "b" be 1.l, body = lambda("x", body = "x".v * ("a".v - "b".v)))
    val f = e.force(StackFrame.root())
    println(f.toExpr())
}

private fun tryParse() {
    val input = CharInput.from("let x = 1 in add 1 x")
    val ts = tokens.parse(input).force()
    println(ts)
    val expr = expr.parse(TokenInput.from(ts)).force()
    println(expr)
}

private fun tryMap() {
    val source = intArrayOf(0, 1, 2, 3, 4).map(::IntLiteral).foldRight(empty()) { x, l -> x cons l }
    val f = lambda("n", body = "n".v * "n".v)
    val expr = let("map" be map, body = "map"(f, source))
    println("Type of $expr = ${inferType(expr)}")
    val result = expr.force()
    println(result)
}

private fun tryFac() {
    val expr = let("fac" be fac, body = apply("fac", 10.l))
    println("Type of \n$expr:\n${inferType(expr)}")
    val result = expr.eval().force()
    println("result = $result")
}