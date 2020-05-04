package hask.core.ast

import hask.core.ast.Expr.*
import hask.core.rt.Value
import hask.core.rt.Value.ADTValue
import hask.core.rt.Value.IntValue
import hask.core.rt.eq
import hask.core.type.Type
import hask.core.type.Type.*

data class Builtin(val name: String, val type: Type) {
    override fun toString(): String = name

    companion object {
        val Boolean = ADT("Boolean", emptyList())

        val True = ADTConstructor(Boolean, "True", emptyList())

        val False = ADTConstructor(Boolean, "False", emptyList())

        val BooleanT = ParameterizedADT(Boolean, emptyList())

        private val INT_OPERATOR = Func(INT, Func(INT, INT))

        val plus = Builtin("add", INT_OPERATOR)

        val minus = Builtin("sub", INT_OPERATOR)

        val times = Builtin("mul", INT_OPERATOR)

        val div = Builtin("div", INT_OPERATOR)

        val eq = Builtin("eq", Func(Var("a"), Func(Var("a"), BooleanT)))

        val listADT = ADT("List", listOf("a"))

        val listT = ParameterizedADT(listADT, listOf(Var("a")))

        val empty = ADTConstructor(listADT, "Empty", emptyList())

        val cons = ADTConstructor(
            listADT, "Cons", listOf(
                Var("a"),
                listT
            )
        )

        fun intOperator(function: (Int, Int) -> Int) =
            Lambda(
                "x",
                Lambda("y", ApplyBuiltin("+", listOf(INT, INT), INT, listOf(ValueOf("x"), ValueOf("y"))) { (x, y) ->
                    val valueX = (x as IntValue).value
                    val valueY = (y as IntValue).value
                    IntValue(function(valueX, valueY))
                })
            )

        fun constant(name: String, value: Value, type: Type) =
            ApplyBuiltin(name, emptyList(), type, emptyList()) { value }

        val equals = Lambda(
            "x",
            Lambda(
                "y",
                ApplyBuiltin("eq", listOf(Var("a"), Var("a")), Var("a"), listOf(ValueOf("x"), ValueOf("y"))) { (x, y) ->
                    if (x.eq(y)) ADTValue(True, emptyList())
                    else ADTValue(False, emptyList())
                })
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
    }
}