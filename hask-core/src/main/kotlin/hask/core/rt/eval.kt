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
import hask.core.rt.NormalForm.*
import hask.core.rt.NormalForm.Function

private sealed class ThunkState

private data class Unevaluated(val frame: StackFrame, val expr: Expr) : ThunkState()

private data class Evaluated(val value: NormalForm) : ThunkState()

class Thunk private constructor(private var state: ThunkState) {
    fun force(): NormalForm = when (val st = state) {
        is Unevaluated -> st.expr.force(st.frame).also { state = Evaluated(it) }
        is Evaluated   -> st.value
    }

    override fun toString(): String = when (val st = state) {
        is Unevaluated -> "unevaluated ${st.expr}}"
        is Evaluated   -> "evaluated ${st.value}"
    }

    companion object {
        fun lazy(frame: StackFrame, expr: Expr) = Thunk(Unevaluated(frame, expr))

        fun strict(value: NormalForm) = Thunk(Evaluated(value))
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

internal fun NormalForm.apply(argument: Thunk): NormalForm {
    check(this is Function) { "Cannot use $this as function" }
    val p = parameters.first()
    val rest = parameters.drop(1)
    val f = frame.withBinding(p, argument)
    return if (rest.isEmpty()) body.force(f)
    else Function(rest, body, f)
}

fun Expr.force(frame: StackFrame = StackFrame.root()): NormalForm = when (this) {
    is IntLiteral      -> IntValue(num)
    is ValueOf         -> frame.getVar(name).force()
    is Lambda          -> Function(parameters, body, frame)
    is Apply           -> l.force(frame).apply(r.eval(frame))
    is Let             -> {
        val newFrame = frame.copy()
        for ((name, value) in bindings) {
            val thunk = value.eval(newFrame)
            newFrame.bindVar(name, thunk)
        }
        body.force(newFrame)
    }
    is If              ->
        if ((cond.force(frame) as ADTValue).constructor == Builtin.True) then.force(frame)
        else otherwise.force(frame)
    is ConstructorCall -> ADTValue(constructor, arguments.map { it.eval(frame) })
    is Match           -> run {
        for ((pattern, body) in arms) {
            when (pattern) {
                is Integer     -> {
                    val value = (expr.force(frame) as IntValue).value
                    if (value == pattern.value) return body.force(frame)
                }
                is Constructor -> {
                    val value = (expr.force(frame) as ADTValue)
                    if (value.constructor == pattern.constructor) {
                        val bindings = pattern.names.zip(value.fields).toMap()
                        return body.force(frame.withBindings(bindings))
                    }
                }
                Otherwise      -> return body.force(frame)
            }
        }
        error("No match")
    }
    is ApplyBuiltin    -> {
        val values = arguments.map { it.force(frame) }
        function(values)
    }
}

fun Expr.eval(frame: StackFrame = StackFrame.root()): Thunk = when (this) {
    is IntLiteral      -> Thunk.strict(IntValue(num))
    is Lambda          -> Thunk.strict(Function(parameters, body, frame))
    is ConstructorCall -> Thunk.strict(ADTValue(constructor, arguments.map { it.eval(frame) }))
    else               -> Thunk.lazy(frame, this)
}