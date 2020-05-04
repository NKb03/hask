/**
 * @author Nikolaus Knop
 */

package hask.core.rt

import hask.core.ast.Builtin
import hask.core.ast.Builtin.Companion.BooleanT
import hask.core.ast.Builtin.Companion.constant
import hask.core.ast.Builtin.Companion.intOperator
import hask.core.ast.Expr
import hask.core.ast.Expr.*
import hask.core.ast.Pattern.*
import hask.core.rt.Thunk.State.Evaluated
import hask.core.rt.Thunk.State.Unevaluated
import hask.core.rt.Value.ADTValue
import hask.core.rt.Value.IntValue

class Thunk private constructor(private var state: State) {
    private sealed class State {
        data class Unevaluated(val parameters: List<String>, val frame: StackFrame, val eval: (StackFrame) -> Thunk) :
            State()

        data class Evaluated(val value: Value) : State()
    }

    fun force(): Value = when (val st = state) {
        is Unevaluated -> {
            check(st.parameters.isEmpty()) { "Cannot force closure with parameters" }
            val value = st.eval(st.frame).force()
            state = Evaluated(value)
            value
        }
        is Evaluated   -> st.value
    }

    fun apply(argument: Thunk): Thunk {
        val st = state
        check(st is Unevaluated && st.parameters.isNotEmpty())
        return function(st.parameters.drop(1), st.frame.withBinding(st.parameters.first(), argument), st.eval)
    }

    companion object {
        fun lazy(frame: StackFrame, eval: (StackFrame) -> Thunk) = Thunk(Unevaluated(emptyList(), frame, eval))

        fun strict(value: Value) = Thunk(Evaluated(value))

        fun function(parameters: List<String>, frame: StackFrame, eval: (StackFrame) -> Thunk) =
            Thunk(Unevaluated(parameters, frame, eval))
    }
}

class StackFrame private constructor(
    private val variables: MutableMap<String, Thunk>,
    private val parent: StackFrame?
) {
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
    is Lambda          -> Thunk.function(parameters, frame) { fr -> body.eval(fr) }
    is Apply           -> l.eval(frame).apply(r.eval(frame))
    is Let             -> {
        val newFrame = frame.copy()
        for ((name, value) in bindings) {
            val thunk = value.eval(newFrame)
            newFrame.bindVar(name, thunk)
        }
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