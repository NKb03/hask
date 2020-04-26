/**
 * @author Nikolaus Knop
 */

package hask.core.rt

import hask.core.ast.*
import hask.core.ast.Expr.*
import hask.core.parse.*
import hask.core.type.Type.ParameterizedADT
import hask.core.rt.Value.IntValue
import hask.core.type.Type
import hask.core.type.inferType

operator fun Expr.minus(r: Expr) = ApplyBuiltin("-", listOf(Type.INT, Type.INT), Type.INT, listOf(this, r)) { (l, r) ->
    IntValue((l as IntValue).value - (r as IntValue).value)
}

operator fun Expr.times(r: Expr) = ApplyBuiltin("*", listOf(Type.INT, Type.INT), Type.INT, listOf(this, r)) { (l, r) ->
    IntValue((l as IntValue).value * (r as IntValue).value)
}

operator fun Expr.invoke(vararg expressions: Expr): Expr = expressions.fold(this) { acc, a -> Apply(acc, a) }

operator fun String.invoke(vararg expressions: Expr): Expr = ValueOf(this).invoke(*expressions)

val fac = Lambda("n",
    Match(
        ValueOf("n"), mapOf(
            Pattern.Integer(0) to IntLiteral(1),
            Pattern.Otherwise to ValueOf("n") * "fac"(ValueOf("n") - IntLiteral(1))
        )
    )
)

val List = ADT("List", listOf("a"))

val Empty = ADTConstructor(List, "Empty", emptyList())

val Cons = ADTConstructor(List, "Cons", listOf(Type.Var("a"), ParameterizedADT(List, listOf(Type.Var("a")))))

infix fun Expr.cons(rest: Expr) = ConstructorCall(Cons, listOf(this, rest))

fun empty() = ConstructorCall(Empty, emptyList())

val map = lambda("f", "list", body = Match(ValueOf("list"), mapOf(
    Pattern.Constructor(Empty, emptyList()) to empty(),
    Pattern.Constructor(Cons, listOf("x", "xs")) to ("f"(ValueOf("x")) cons "map"(ValueOf("f"), ValueOf("xs")))
)))

fun main() {
    tryMap()
    val input = CharInput.from("let x = 1 in add 1 x")
    val ts = tokens.parse(input).force()
    println(ts)
    val expr = expr.parse(TokenInput.from(ts)).force()
    println(expr)
}

private fun tryMap() {
    val source = intArrayOf(0, 1, 2, 3, 4).map(::IntLiteral).foldRight(empty()) { x, l -> x cons l }
    val f = lambda("n", body = ValueOf("n") * ValueOf("n"))
    val expr = let("map" be map, body= "map"(f, source))
    println("Type of $expr = ${inferType(expr)}")
    val result = expr.eval().force()
    println(result)
}

private fun tryFac() {
    val expr = let("fac" be fac, body = apply("fac", IntLiteral(10)))
    println("Type of \n$expr:\n${inferType(expr)}")
    val result = expr.eval().force()
    println("result = $result")
}