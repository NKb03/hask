/**
 *@author Nikolaus Knop
 */

package hask.core

import hask.core.ast.*
import hask.core.ast.Expr.If
import hask.core.type.Type.*
import hask.core.type.TypeScheme
import hask.core.type.inferType
import org.junit.Test

class TypeInferenceTest {
    @Test fun `factorial function`() {
        val expr = let(
            "fac" be lambda(
                "n",
                body = apply("mul", "n".v, apply("fac", apply("sub", "n".v, 1.l)))
            ),
            body = "fac".v
        )
        val type = inferType(expr)
        type shouldPrint "int -> int"
    }

    @Test fun `simple let`() {
        val expr = let(
            "double" be lambda("n", body = apply("mul", "n".v, 2.l)),
            body = "double".v
        )
        val type = inferType(expr)
        type shouldPrint "int -> int"
    }

    @Test fun `occurs check`() {
        val expr = let("x" be apply("x", "x".v), body = "x".v);
        { inferType(expr) }.shouldThrow(RuntimeException::class)
    }

    @Test fun `list map`() {
        val expr = let(
            "map" be lambda(
                "l", "f",
                body = If(
                    apply("isEmpty", "l".v),
                    "Empty".v,
                    apply("Cons", apply("f", apply("head", "l".v)), apply("map", apply("tail", "l".v), "f".v))
                )
            ),
            body = "map".v
        )
        val type = inferType(expr)
        val list = ADT("List", listOf("a"))
        val expected = TypeScheme(
            listOf("a", "b"),
            Func(
                ParameterizedADT(list, listOf(Var("a"))),
                Func(
                    Func(Var("a"), Var("b")),
                    ParameterizedADT(list, listOf(Var("b")))
                )
            )
        )
        type shouldMatch expected
    }

    @Test fun `partial multiplication application`() {
        val expr = let(
            "id" be lambda("x", body = apply("mul", "x".v, 1.l)),
            body = apply("id", 3.l)
        )
        inferType(expr) shouldPrint "int"
    }

    @Test fun `use of binding in binding on same level`() {
        val e = let(
            "a" be apply("id", 1.l),
            "id" be lambda("x", body = "x".v),
            body = "id".v
        )
        inferType(e) shouldMatch "a -> a"
    }

    @Test fun `mutual recursive definitions in let`() {
        val e = let(
            "f" be lambda("x", body = apply("g", 1.l)),
            "g" be lambda("x", body = apply("f", "x".v)),
            body = "g".v
        )
        inferType(e) shouldMatch "int -> a"
    }

    @Test fun `recursive application with concrete argument type`() {
        val e = let("id" be lambda("x", body = apply("id", 1.l)), body = "id".v)
        inferType(e) shouldMatch "int -> a"
    }

    @Test fun `recursive applications with different arguments`() {
        val e = let(
            "f" be lambda(
                "x", body = let(
                    "a" be apply("f", 1.l),
                    body = apply("f", "a".v)
                )
            ),
            body = "f".v
        )
        //TODO(what is this supposed to do?)
    }

    @Test fun `funny expression`() {
        val e = lambda("x", body = apply("x", lambda("x", body = "x".v)))
        inferType(e) shouldMatch "((a -> a) -> b) -> b"
    }
}