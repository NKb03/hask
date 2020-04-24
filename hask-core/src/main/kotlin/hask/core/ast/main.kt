/**
 * @author Nikolaus Knop
 */

package hask.core.ast

import hask.core.ast.Expr.*
import hask.core.ast.Pattern.Constructor
import hask.core.rt.invoke
import hask.core.type.*
import hask.core.type.Type.Var

fun main() {
    testOrDefault()
}

private fun testBox() {
    val box = ADT("Box", listOf("a"))
    val boxC = ADTConstructor(box, "Box", listOf(Var("a")))
    val expr = Let("f", lambda("a", "b", body = ValueOf("a")), "f"(IntLiteral(1), IntLiteral(2)))
    println(inferType(expr))
}

private fun testOrDefault() {
    val maybe = ADT("Maybe", listOf("a"))
    val just = ADTConstructor(maybe, "Just", listOf(Var("a")))
    val nothing = ADTConstructor(maybe, "Nothing", emptyList())
    val orDefault = Lambda(
        "default",
        Lambda(
            "opt",
            Match(
                ValueOf("opt"),
                mapOf(
                    Constructor(just, listOf("value")) to ValueOf("value"),
                    Constructor(nothing, emptyList()) to ValueOf("default")
                )
            )
        )
    )
    val expr = Let("orDefault", orDefault, "orDefault"(IntLiteral(0), ConstructorCall(just, listOf(IntLiteral(1)))))
    val type = inferType(expr)
    println("tpe = $type")
}