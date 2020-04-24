/**
 * @author Nikolaus Knop
 */

package hask.core.type

import hask.core.ast.Builtin
import hask.core.ast.Expr
import hask.core.ast.Expr.*
import hask.core.type.Type.*
import java.util.*

fun Expr.inferType(
    env: Env,
    namer: Namer,
    constraints: MutableList<Constraint>
): Type = when (this) {
    is IntLiteral      -> INT
    is ValueOf         -> env[name]?.instantiate(namer) ?: error("Unbound identifier $name")
    is Lambda          -> {
        val tyVar = Var(namer.freshName())
        val newEnv = env + (parameter to TypeScheme(emptySet(), tyVar))
        val bodyT = body.inferType(newEnv, namer, constraints)
        Func(tyVar, bodyT)
    }
    is Apply           -> {
        val lt = l.inferType(env, namer, constraints)
        val rt = r.inferType(env, namer, constraints)
        val ret = Var(namer.freshName())
        constraints.bind(lt, Func(rt, ret))
        ret
    }
    is Let             -> {
        val defTypeVar = Var(namer.freshName())
        val env1 = env + (name to TypeScheme(emptySet(), defTypeVar))
        val defType = value.inferType(env1, namer, constraints)
        val principal = principalType(defType, env1, constraints)
        val env2 = env + (name to principal)
        constraints.bind(defType, defTypeVar)
        body.inferType(env2, namer, constraints)
    }
    is If              -> {
        val ret = Var(namer.freshName())
        val condT = cond.inferType(env, namer, constraints)
        constraints.bind(condT, Builtin.BooleanT)
        val t1 = then.inferType(env, namer, constraints)
        constraints.bind(t1, ret)
        val t2 = otherwise.inferType(env, namer, constraints)
        constraints.bind(t2, ret)
        ret
    }
    is ConstructorCall -> {
        val typeArguments = mutableMapOf<String, Type>()
        constructor.parameters.zip(arguments) { p, a ->
            if (p is Var) {
                val fresh = typeArguments.getOrPut(p.name) { Var(namer.freshName()) }
                constraints.add(Constraint(a.inferType(env, namer, constraints), fresh))
            } else {
                constraints.add(Constraint(a.inferType(env, namer, constraints), p))
            }
        }
        val positionalTypeArgs = constructor.adt.typeParameters.map {
            typeArguments.getOrElse(it) {
                Var(namer.freshName())
            }
        }
        ParameterizedADT(constructor.adt, positionalTypeArgs)
    }
    is Match           -> {
        val matchedType = expr.inferType(env, namer, constraints)
        val common = Var(namer.freshName())
        for ((pattern, body) in arms) {
            val expectedType = pattern.inferExpectedType(namer)
            val returnType = body.inferType(env + pattern.defineVars(namer), namer, constraints)
            constraints.add(Constraint(matchedType, expectedType))
            constraints.add(Constraint(common, returnType))
        }
        common
    }
    is ApplyBuiltin    -> {
        parameters.zip(arguments) { p, a ->
            constraints.add(Constraint(a.inferType(env, namer, constraints), p))
        }
        returnType
    }
}

private fun unify(a: Type, b: Type, subst: MutableMap<String, Type>) {
    when {
        a == b                                                           -> {
        }
        a is Var && a.name !in b.fvs()                                   -> {
            subst[a.name] = b
            subst.entries.forEach { e ->
                e.setValue(e.value.apply(mapOf(a.name to b)))
            }
        }
        b is Var && b.name !in a.fvs()                                   -> {
            subst.entries.forEach { e ->
                e.setValue(e.value.apply(mapOf(b.name to a)))
            }
            subst[b.name] = a
        }
        a is Func && b is Func                                           -> {
            unify(a.from, b.from, subst)
            unify(a.to.apply(subst), b.to.apply(subst), subst)
        }
        a is ParameterizedADT && b is ParameterizedADT && a.adt == b.adt -> {
            a.typeArguments.zip(b.typeArguments).forEach { (s, t) ->
                unify(s.apply(subst), t.apply(subst), subst)
            }
        }
        else                                                             -> error("Cannot solve constraint $a = $b")
    }
}

fun unify1(constraints: Constraints): Subst {
    val subst = mutableMapOf<String, Type>()
    for ((a, b) in constraints) {
        unify(a.apply(subst), b.apply(subst), subst)
    }
    return subst
}

fun unify(constraints: Constraints): Subst {
    if (constraints.isEmpty()) return emptyMap()
    val (l, r) = constraints.first()
    val rest = constraints.drop(1)
    return when {
        l == r                                                           -> unify(rest)
        l is Var                                                         -> bind(l, r, rest)
        r is Var                                                         -> bind(r, l, rest)
        l is Func && r is Func                                           -> {
            val additional = listOf(Constraint(l.from, r.from), Constraint(l.to, r.to))
            unify(rest + additional)
        }
        l is ParameterizedADT && r is ParameterizedADT && l.adt == r.adt -> {
            val additional = l.typeArguments.zip(r.typeArguments) { s, t -> Constraint(s, t) }
            unify(rest + additional)
        }
        else                                                             -> error("Cannot solve constraint $l = $r")
    }
}

private fun bind(
    r: Var,
    l: Type,
    rest: List<Constraint>
): Map<String, Type> = if (r.name in l.fvs()) error("Occurs check failed") else {
    val subst = mapOf(r.name to l)
    unify(rest.apply(subst)) compose subst
}

fun principalType(type: Type, env: Env, constraints: Constraints): TypeScheme {
    val subst = unify(constraints)
    return type.apply(subst).generalize(env)
}

fun inferType(
    expr: Expr
): TypeScheme {
    val env = Builtin.env
    val namer = SimpleNamer()
    val constraints = LinkedList<Constraint>()
    val general = expr.inferType(env, namer, constraints)
    val subst = unify1(constraints)
    val specialized = general.apply(subst)
    return specialized.generalize(env)
}