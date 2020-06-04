/**
 * @author Nikolaus Knop
 */

package hask.core.rt

import hask.core.ast.*
import hask.core.ast.Expr.*
import hask.core.ast.Pattern.*
import hask.core.rt.NormalForm.*
import hask.core.rt.NormalForm.Function
import hask.core.topologicalSort
import hask.core.type.*

fun Expr.force(frame: StackFrame): NormalForm = when (this) {
    is IntLiteral   -> num?.let { IntValue(it) } ?: Irreducible(this)
    is ValueOf      -> frame.getVar(name).force()
    is Lambda       -> Function(parameters, body, frame)
    is Apply        -> function.apply(arguments, frame)
    is Let          -> {
        val newFrame = frame.child()
        for ((name, value) in bindings) {
            newFrame.put(name, value.eval(newFrame))
        }
        body.force(newFrame)
    }
    is If           ->
        if ((cond.force(frame) as ADTValue).constructor == Builtin.True) then.force(frame)
        else otherwise.force(frame)
    is Match        -> run {
        val subject = expr.force(frame)
        for ((pattern, body) in arms) {
            val f = frame.child()
            if (pattern.match(subject, f)) return body.force(f)
        }
        error("No match")
    }
    is ApplyBuiltin -> function(arguments.map { it.eval(frame) })
    is Hole         -> Irreducible(this)
}

fun Expr.evaluate(tl: TopLevelEnv = TopLevelEnv(emptyList())): NormalForm {
    val frame = StackFrame.root()
    tl.putConstructorFunctions(frame)
    return force(frame)
}

private fun Pattern.match(subject: NormalForm, frame: StackFrame): Boolean = when (this) {
    Wildcard         -> true
    is Variable      -> {
        frame.put(name, Thunk.strict(subject))
        true
    }
    is Integer       -> {
        check(subject is IntValue) { "Unexpected subject $subject" }
        subject.value == value.toInt()
    }
    is Destructuring -> run {
        check(subject is ADTValue) { "Unexpected subject $subject" }
        if (subject.constructor.name != constructor) return false
        for ((v, p) in subject.fields.zip(components)) {
            if (!p.match(v.force(), frame)) return false
        }
        return true
    }
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
    is IntLiteral -> Thunk.strict(force(frame))
    is Lambda     -> Thunk.strict(Function(parameters, body, frame))
    else          -> Thunk.lazy(frame, this)
}

fun Expr.substitute(subst: Map<String, Expr>): Expr = when (this) {
    is IntLiteral   -> this
    is ValueOf      -> subst[name] ?: this
    is Lambda       -> Lambda(parameters, body.substitute(subst - parameters))
    is Apply        -> Apply(function.substitute(subst), arguments.map { it.substitute(subst) })
    is Let          -> {
        val s = subst - bindings.map { it.name }
        Let(bindings.map { (n, v) -> Binding(n, v.substitute(s)) }, body.substitute(s))
    }
    is If           -> If(cond.substitute(subst), then.substitute(subst), otherwise.substitute(subst))
    is Match        -> Match(
        expr.substitute(subst),
        arms.mapValues { (p, v) -> v.substitute(subst - p.boundVariables()) }
    )
    is ApplyBuiltin -> ApplyBuiltin(name, parameters, returnType, arguments.map { it.substitute(subst) }, function)
    is Hole         -> this
}

fun Expr.isNormalForm() = when (this) {
    is IntLiteral -> num != null
    is Lambda     -> true
    else          -> false
}

fun Expr.evaluateOnce(env: Map<String, Expr>): Expr? = when (this) {
    is ValueOf      -> env[name] ?: ValueOf(name)
    is Apply        -> when (val f = function) {
        is Lambda  -> f.body.substitute(f.parameters.zip(arguments).toMap())
        is ValueOf -> run {
            if (arguments.any { !it.isNormalForm() }) return null
            val func = Builtin.prelude[f.name] as? Lambda ?: return null
            val builtin = func.body as? ApplyBuiltin ?: return null
            builtin.function(arguments.map { it.eval() }).toExpr(env.keys)
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
            val values = arguments.map { it.eval() }
            function(values).toExpr(env.keys)
        } else null
    }
    else            -> null
}
