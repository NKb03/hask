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
        fun root() = StackFrame(mutableMapOf(), null).apply {
            for ((n, v) in Builtin.prelude) {
                put(n, v.eval(this))
            }
        }
    }
}