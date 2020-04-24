/**
 * @author Nikolaus Knop
 */

package hask.core.rt

import hask.core.ast.*
import hask.core.ast.Builtin.Companion
import hask.core.ast.Builtin.Companion.BooleanT
import hask.core.ast.Builtin.Companion.constant
import hask.core.ast.Builtin.Companion.intOperator
import hask.core.ast.Expr.*
import hask.core.ast.Pattern.*
import hask.core.rt.Thunk.State.Evaluated
import hask.core.rt.Thunk.State.Unevaluated
import hask.core.rt.Value.ADTValue
import hask.core.rt.Value.IntValue

class Thunk private constructor(private var state: State) {
    private sealed class State {
        data class Unevaluated(val parameter: String?, val frame: StackFrame, val eval: (StackFrame) -> Thunk) :
            State()

        data class Evaluated(val value: Value) : State()
    }

    fun force(): Value = when (val st = state) {
        is Unevaluated -> {
            check(st.parameter == null) { "Cannot force closure with parameters" }
            val value = st.eval(st.frame).force()
            state = Evaluated(value)
            value
        }
        is Evaluated   -> st.value
    }

    fun apply(argument: Thunk): Thunk {
        val st = state
        check(st is Unevaluated && st.parameter != null)
        return st.eval(st.frame.withBinding(st.parameter, argument))
    }

    companion object {
        fun lazy(frame: StackFrame, eval: (StackFrame) -> Thunk) = Thunk(Unevaluated(null, frame, eval))

        fun strict(value: Value) = Thunk(Evaluated(value))

        fun function(parameter: String, frame: StackFrame, eval: (StackFrame) -> Thunk) =
            Thunk(Unevaluated(parameter, frame, eval))
    }
}

class StackFrame private constructor(private val variables: MutableMap<String, Thunk>, private val parent: StackFrame?) {
    fun withBinding(name: String, thunk: Thunk) = StackFrame(mutableMapOf(name to thunk), parent = this)

    fun copy() = StackFrame(variables.toMutableMap(), parent)

    fun bindVar(name: String, thunk: Thunk) {
        variables[name] = thunk
    }

    fun withBindings(bindings: Map<String, Thunk>) = copy().also { it.variables.putAll(bindings) }

    fun getVar(name: String): Thunk =
        variables[name] ?: parent?.getVar(name) ?: throw NoSuchElementException("Variable $name is not bound")

    companion object {
        private val prelude = mutableMapOf(
            "add" to intOperator(Int::plus).eval(),
            "mul" to intOperator(Int::times).eval(),
            "sub" to intOperator(Int::minus).eval(),
            "div" to intOperator(Int::div).eval(),
            "False" to constant("False", ADTValue(Builtin.False, emptyList()), BooleanT).eval(),
            "True" to constant("True", ADTValue(Builtin.True, emptyList()), BooleanT).eval(),
            "eq" to Builtin.equals.eval()
        )

        fun root() = StackFrame(prelude, null)
    }
}

fun Expr.eval(frame: StackFrame = StackFrame.root()): Thunk = when (this) {
    is IntLiteral      -> Thunk.strict(IntValue(num))
    is ValueOf         -> frame.getVar(name)
    is Lambda          -> Thunk.function(parameter, frame) { fr -> body.eval(fr) }
    is Apply           -> l.eval(frame).apply(r.eval(frame))
    is Let             -> {
        val newFrame = frame.copy()
        val thunk = value.eval(newFrame)
        newFrame.bindVar(name, thunk)
        body.eval(newFrame)
    }
    is If              ->
        if ((cond.eval(frame).force() as ADTValue).constructor == Builtin.True)
            then.eval(frame)
        else otherwise.eval(frame)
    is ConstructorCall -> Thunk.strict(ADTValue(constructor, arguments.map { it.eval(frame) }))
    is Match           -> Thunk.lazy(frame) {
        for ((pattern, body) in arms) {
            when (pattern) {
                is Integer     -> {
                    val value = (expr.eval(frame).force() as IntValue).value
                    if (value == pattern.value) return@lazy body.eval(frame)
                }
                is Constructor -> {
                    val value = (expr.eval(frame).force() as ADTValue)
                    if (value.constructor == pattern.constructor) {
                        val bindings = pattern.names.zip(value.fields).toMap()
                        return@lazy body.eval(frame.withBindings(bindings))
                    }
                }
                Otherwise      -> return@lazy body.eval(frame)
            }
        }
        error("No match")
    }
    is ApplyBuiltin    -> {
        val values = arguments.map { it.eval(frame).force() }
        Thunk.strict(function(values))
    }
}