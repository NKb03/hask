/**
 * @author Nikolaus Knop
 */

package hask.core.ast

import hask.core.ast.Expr.*
import hask.core.ast.Pattern.Destructuring
import hask.core.ast.Pattern.Variable
import hask.core.rt.evaluate
import hask.core.rt.invoke
import hask.core.type.TopLevelEnv
import hask.core.type.Type.Var
import hask.core.type.inferType

data class Program(val adtDefs: List<ADTDef>, val expr: Expr)

fun main() {
    testOrDefault()
}

private fun testBox() {
    val box = ADT("Box", listOf("a"))
    val boxC = ADTConstructor("Box", listOf(Var("a")))
    val expr = let("f" be lambda("a", "b", body = "a".v), body = "f"(1.l, 2.l))
    val tl = TopLevelEnv(listOf(ADTDef(box, listOf(boxC))))
    println(inferType(expr, tl))
}

private fun testOrDefault() {
    val maybe = ADT("Maybe", listOf("a"))
    val just = ADTConstructor("Just", listOf(Var("a")))
    val nothing = ADTConstructor("Nothing", emptyList())
    val orDefault = lambda(
        "default", "opt",
        body = Match(
            "opt".v,
            mapOf(
                Destructuring("Just", listOf(Variable("value"))) to "value".v,
                Destructuring("Nothing", emptyList()) to "default".v
            )
        )
    )
    val expr = let(
        "orDefault" be orDefault,
        body = "orDefault"(0.l, apply("Just", 1.l))
    )
    val tl = TopLevelEnv(listOf(ADTDef(maybe, listOf(just, nothing))))
    val type = inferType(expr, tl)
    println("tpe = $type")
    println(expr.evaluate(tl))
}