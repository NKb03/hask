/**
 * @author Nikolaus Knop
 */

package hask.core.rt

import hask.core.ast.Builtin
import hask.core.ast.Expr
import hask.core.ast.Expr.*
import hask.core.ast.Pattern.*
import hask.core.rt.NormalForm.*
import hask.core.rt.NormalForm.Function


fun Expr.force(frame: StackFrame = StackFrame.root()): NormalForm = when (this) {
    is IntLiteral      -> IntValue(num)
    is ValueOf         -> frame.getVar(name).force()
    is Lambda          -> Function(parameters, body, frame)
    is Apply           -> {
        val f = function.force(frame)
        check(f is Function) { "Cannot use $this as function" }
        val args = arguments.map { it.eval(frame) }
        val newFrame = frame.withBindings(f.parameters.zip(args))
        val rest = f.parameters.drop(args.size)
        if (rest.isEmpty()) f.body.force(newFrame)
        else Function(rest, f.body, newFrame)
    }
    is Let             -> {
        val newFrame = frame.child()
        for ((name, value) in bindings) {
            newFrame.put(name, value.eval(newFrame))
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
                        val bindings = pattern.names.zip(value.fields)
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

fun Expr.substitute(subst: Map<String, Expr>): Expr = when (this) {
    is IntLiteral      -> this
    is ValueOf         -> subst[name] ?: this
    is Lambda          -> Lambda(parameters, body.substitute(subst - parameters))
    is Apply           -> Apply(function.substitute(subst), arguments.map { it.substitute(subst) })
    is Let             -> {
        val s = subst - bindings.map { it.name }
        Let(bindings.map { (n, v) -> Binding(n, v.substitute(s)) }, body.substitute(s))
    }
    is If              -> If(cond.substitute(subst), then.substitute(subst), otherwise.substitute(subst))
    is ConstructorCall -> ConstructorCall(constructor, arguments.map { it.substitute(subst) })
    is Match           -> Match(
        expr.substitute(subst),
        arms.mapValues { (p, v) -> v.substitute(subst - p.boundVariables()) }
    )
    is ApplyBuiltin    -> ApplyBuiltin(name, parameters, returnType, arguments.map { it.substitute(subst) }, function)
}