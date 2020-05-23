/**
 *@author Nikolaus Knop
 */

package hask.core

import hask.core.ast.*
import org.spekframework.spek2.Spek

class EvaluationTest : Spek({
    assertEvaluatesTo(1, apply(apply("id", "id".v), 1.l))
    assertEvaluatesTo(1, apply("id", "id".v, 1.l))
    assertEvaluatesTo(1, apply("id", apply("id", "id".v), apply("id", "id".v, 1.l)))
    assertEvaluatesTo(3, apply("add", 1.l, 2.l))
    assertEvaluatesTo("True", apply("eq", apply("add", 1.l, 2.l), apply("add", 2.l, 1.l)))
    assertEvaluatesTo(
        9, let(
            "f" be lambda("x", "y", body = apply("add", apply("g", "x".v, 3.l), apply("g", "y".v, 2.l))),
            "g" be lambda("y", "x", body = apply("sub", apply("mul", 2.l, "y".v), "x".v)),
            body = apply("f", 3.l, 4.l)
        )
    )
})