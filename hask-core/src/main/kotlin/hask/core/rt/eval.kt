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
import hask.core.topologicalSort
import hask.core.type.makeGraph

fun Expr.force(frame: StackFrame = StackFrame.root()): NormalForm = when (this) {
    is IntLiteral      -> num?.let { IntValue(it) } ?: Irreducible(this)
    is ValueOf         -> frame.getVar(name).force()
    is Lambda          -> Function(parameters, body, frame)
    is Apply           -> function.apply(arguments, frame)
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
    is ApplyBuiltin    -> function(arguments.map { it.force(frame) })
    is Hole       -> Irreducible(this)
}

private fun Expr.apply(arguments: List<Expr>, frame: StackFrame): NormalForm {
    return when (val f = force(frame)) {
        is Irreducible -> Irreducible(Apply(this, arguments))
        is Function    -> f.apply(arguments, frame)
        else           -> error("Cannot use $f as function")
    }
}

fun Function.apply(arguments: List<Expr>, frame: StackFrame): NormalForm {
    val newFrame = this.frame.child()
    for ((p, a) in parameters.zip(arguments)) newFrame.put(p, a.eval(frame))
    return when {
        parameters.size > arguments.size -> Function(parameters.drop(arguments.size), body, newFrame)
        parameters.size < arguments.size -> body.force(newFrame).toExpr(frame.boundVariables())
            .apply(arguments.drop(parameters.size), frame)
        else                             -> body.force(newFrame)
    }
}

fun Expr.eval(frame: StackFrame = StackFrame.root()): Thunk = when (this) {
    is IntLiteral      -> Thunk.strict(force(frame))
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
    is Hole       -> this
}

fun Expr.isNormalForm() = when (this) {
    is IntLiteral      -> num != null
    is Lambda          -> true
    is ConstructorCall -> true
    else               -> false
}

fun Expr.evaluateOnce(env: Map<String, Expr>): Expr? = when (this) {
    is ValueOf      -> env[name] ?: ValueOf(name)
    is Apply        -> when (val f = function) {
        is Lambda  -> f.body.substitute(f.parameters.zip(arguments).toMap())
        is ValueOf -> run {
            if (arguments.any { !it.isNormalForm() }) return null
            val func = Builtin.prelude[f.name] as? Lambda ?: return null
            val builtin = func.body as? ApplyBuiltin ?: return null
            builtin.function(arguments.map { it.force() }).toExpr(env.keys)
        }
        else       -> null
    }
    is Let          -> {
        val g = makeGraph(env.keys)
        val ts = g.topologicalSort()
        val newEnv = mutableMapOf<String, Expr>()
        for (i in ts) {
            val (n, v) = bindings[i]
            newEnv[n] = v.substitute(newEnv)
        }
        body.substitute(newEnv)
    }
    is If           -> when (cond) {
        ValueOf("True")  -> then
        ValueOf("False") -> otherwise
        else             -> null
    }
    is ApplyBuiltin -> {
        if (arguments.all { it.isNormalForm() }) {
            val values = arguments.map { it.force() }
            function(values).toExpr(env.keys)
        } else null
    }
    else            -> null
}
