package hask.core.rt

import hask.core.ast.Builtin
import hask.core.ast.Builtin.Companion
import hask.core.rt.NormalForm.ADTValue

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
        variables[name] ?: parent?.getVar(name) ?: throw NoSuchElementException("Variable $name is not bound")

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

    companion object {
        private val prelude = mutableMapOf(
            "add" to Builtin.intOperator(Int::plus).eval(),
            "mul" to Builtin.intOperator(Int::times).eval(),
            "sub" to Builtin.intOperator(Int::minus).eval(),
            "div" to Builtin.intOperator(Int::div).eval(),
            "False" to Builtin.constant(
                "False",
                ADTValue(
                    Builtin.False,
                    emptyList()
                ),
                Builtin.BooleanT
            ).eval(),
            "True" to Builtin.constant(
                "True",
                ADTValue(
                    Builtin.True,
                    emptyList()
                ),
                Builtin.BooleanT
            ).eval(),
            "eq" to Builtin.equals.eval()
        )

        fun root() = StackFrame(prelude, null)
    }
}