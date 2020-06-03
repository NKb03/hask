/**
 * @author Nikolaus Knop
 */

package hask.core.ast

import hask.core.ast.Expr.*
import hask.core.ast.Pattern.Constructor
import hask.core.rt.invoke
import hask.core.type.Type.Var
import hask.core.type.inferType

data class Program(val adtDefs: List<ADTDef>, val expr: Expr)

fun main() {
    testOrDefault()
}

private fun testBox() {
    val box = ADT("Box", listOf("a"))
    val boxC = ADTConstructor("Box", listOf(Var("a")))
    val expr = let("f" be lambda("a", "b", body = "a".v), body = "f"(IntLiteral(1), IntLiteral(2)))
    println(inferType(expr, listOf(ADTDef(box, listOf(boxC)))))
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
                Constructor(just, listOf("value")) to "value".v,
                Constructor(nothing, emptyList()) to "default".v
            )
        )
    )
    val expr = let(
        "orDefault" be orDefault,
        body = "orDefault"(IntLiteral(0), ConstructorCall(just, listOf(IntLiteral(1))))
    )
    val type = inferType(expr, listOf(ADTDef(maybe, listOf(just, nothing))))
    println("tpe = $type")
}