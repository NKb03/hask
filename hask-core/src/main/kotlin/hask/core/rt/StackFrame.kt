package hask.core.rt

import hask.core.ast.*
import hask.core.ast.Expr.ValueOf
import hask.core.rt.NormalForm.ADTValue
import hask.core.rt.NormalForm.Irreducible

class StackFrame private constructor(
    private val variables: MutableMap<String, Thunk>,
    private val parent: StackFrame?
) {
    fun withBindings(bindings: Collection<Pair<String, Thunk>>) =
        StackFrame(bindings.toMap(mutableMapOf()), parent = this)

    fun child() = StackFrame(mutableMapOf(), parent = this)

    fun put(name: String, value: Thunk) {
        variables[name] = value
    }

    fun getVar(name: String): Thunk =
        variables[name] ?: parent?.getVar(name) ?: Thunk.strict(Irreducible(ValueOf(name)))

    override fun toString(): String = buildString {
        var f = this@StackFrame
        while (true) {
            println(f.variables)
            for ((name, value) in f.variables) {
                append("$name = $value")
            }
            f = f.parent ?: break
        }
    }

    fun bindings(): Map<String, NormalForm> {
        val res = mutableMapOf<String, NormalForm>()
        var f = this
        while (true) {
            for ((name, value) in variables) {
                if (name !in res) res[name] = value.force()
            }
            f = f.parent ?: break
        }
        return res
    }

    fun boundVariables(): Set<String> {
        val res = mutableSetOf<String>()
        var f = this
        while (true) {
            res.addAll(variables.keys)
            f = f.parent ?: break
        }
        return res
    }

    companion object {
        private val prelude = mutableMapOf(
            "add" to Builtin.intOperator(Int::plus),
            "mul" to Builtin.intOperator(Int::times),
            "sub" to Builtin.intOperator(Int::minus),
            "div" to Builtin.intOperator(Int::div)(),
            "id" to lambda("x", body = "x".v),
            "False" to Builtin.constant(
                "False",
                ADTValue(
                    Builtin.False,
                    emptyList()
                ),
                Builtin.BooleanT
            ),
            "True" to Builtin.constant(
                "True",
                ADTValue(
                    Builtin.True,
                    emptyList()
                ),
                Builtin.BooleanT
            ),
            "eq" to Builtin.equals
        )

        fun root() = StackFrame(mutableMapOf(), null).apply {
            for ((n, v) in prelude) {
                put(n, v.eval(this))
            }
        }
    }
}