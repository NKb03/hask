/**
 *@author Nikolaus Knop
 */

package hask.core

import hask.core.ast.Expr.*
import hask.core.type.inferType
import org.junit.Test

class TypeInferenceTest {
    @Test fun `factorial function`() {
        val expr = Let(
            "fac",
            Lambda(
                "n",
                apply("mul", "n".v, apply("fac", apply("sub", "n".v, 1.l)))
                //                If(
                //                    apply("eq", "n".v, 0.l),
                //                    then = 1.l,
                //                    otherwise = apply("*", "n".v, apply("fac", apply("-", "n".v, 1.l)))
                //                )
            ),
            "fac".v
        )
        val type = inferType(expr)
        type shouldPrint "int -> int"
    }

    @Test fun `simple let`() {
        val expr = Let(
            "double", Lambda("n", apply("mul", "n".v, 2.l)),
            "double".v
        )
        val type = inferType(expr)
        type shouldPrint "int -> int"
    }

    @Test fun `occurs check`() {
        val expr = Let("x", apply("x", "x".v), "x".v);
        { inferType(expr) }.shouldThrow(RuntimeException::class)
    }

    @Test fun `list map`() {
        val expr = Let(
            "map",
            lambda(
                "l", "f", body =
                If(
                    apply("isEmpty", "l".v),
                    "Empty".v,
                    apply("Cons", apply("f", apply("head", "l".v)), apply("map", apply("tail", "l".v), "f".v))
                )
            ),
            "map".v
        )
        val type = inferType(expr)
        type shouldPrint "forall r0 s0 . List r0 -> (r0 -> s0) -> List s0"
    }

    @Test fun `partial multiplication application`() {
        val expr = Let(
            "id",
            lambda("x", body = apply("mul", "x".v, 1.l)),
            apply("id", 3.l)
        )
        inferType(expr) shouldPrint "int"
    }
}