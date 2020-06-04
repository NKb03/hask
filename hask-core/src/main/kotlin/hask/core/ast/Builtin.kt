package hask.core.ast

import hask.core.ast.Expr.ApplyBuiltin
import hask.core.rt.NormalForm
import hask.core.rt.NormalForm.ADTValue
import hask.core.rt.NormalForm.IntValue
import hask.core.rt.invoke
import hask.core.type.Type
import hask.core.type.Type.*

data class Builtin(val name: String, val type: Type) {
    override fun toString(): String = name

    companion object {
        val Boolean = ADT("Boolean", emptyList())

        val True = ADTConstructor("True", emptyList())

        val False = ADTConstructor("False", emptyList())

        val BooleanT = ParameterizedADT("Boolean", emptyList())

        private val INT_OPERATOR = Func(INT, Func(INT, INT))

        val plus = Builtin("add", INT_OPERATOR)

        val minus = Builtin("sub", INT_OPERATOR)

        val times = Builtin("mul", INT_OPERATOR)

        val div = Builtin("div", INT_OPERATOR)

        val eq = Builtin("eq", Func(Var("a"), Func(Var("a"), BooleanT)))

        val listADT = ADT("List", listOf("a"))

        val listT = ParameterizedADT("List", listOf(Var("a")))

        val empty = ADTConstructor("Empty", emptyList())

        val cons = ADTConstructor(
            "Cons", listOf(
                Var("a"),
                listT
            )
        )

        val adtDefinitions = listOf(
            ADTDef(Boolean, listOf(True, False)),
            ADTDef(listADT, listOf(empty, cons))
        )

        fun intOperator(function: (Int, Int) -> Int) =
            lambda(
                "x",
                "y",
                body = ApplyBuiltin("+", listOf(INT, INT), INT, listOf("x".v, "y".v)) { (x, y) ->
                    val valueX = (x as IntValue).value
                    val valueY = (y as IntValue).value
                    IntValue(function(valueX, valueY))
                })


        fun constant(name: String, value: NormalForm, type: Type) =
            ApplyBuiltin(name, emptyList(), type, emptyList()) { value }

        val equals = lambda(
            "x",
            "y",
            body = ApplyBuiltin(
                "eq",
                listOf(Var("a"), Var("a")),
                Var("a"),
                listOf("x".v, "y".v)
            ) { (x, y) ->
                if (x.eq(y)) ADTValue(True, emptyList())
                else ADTValue(False, emptyList())
            }
        )

        val emptyF = constructorFunc(listADT, empty)

        val consF = constructorFunc(listADT, cons)

        val isEmpty = Builtin("isEmpty", Func(listT, BooleanT))

        val head = Builtin("head", Func(listT, Var("a")))

        val tail = Builtin(
            "tail",
            Func(listT, listT)
        )

        private val all = listOf(
            plus,
            minus,
            times,
            div,
            eq,
            emptyF,
            consF,
            isEmpty,
            head,
            tail
        )

        val env = env(all)

        fun env(builtin: List<Builtin>) = builtin.associate { it.name to it.type.generalize(emptySet()) }

        val prelude = mapOf(
            "add" to intOperator(Int::plus),
            "mul" to intOperator(Int::times),
            "sub" to intOperator(Int::minus),
            "div" to intOperator(Int::div)(),
            "False" to constant(
                "False",
                ADTValue(
                    False,
                    emptyList()
                ),
                BooleanT
            ),
            "True" to constant(
                "True",
                ADTValue(
                    True,
                    emptyList()
                ),
                BooleanT
            ),
            "eq" to equals
        )
    }
}