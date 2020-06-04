/**
 * @author Nikolaus Knop
 */

package hask.core.type

import hask.core.ast.*
import hask.core.ast.Expr.*
import hask.core.condense
import hask.core.topologicalSort
import hask.core.type.Type.*
import java.util.*

fun Expr.freeVariables(env: Set<String>, collect: MutableSet<String> = mutableSetOf()): Set<String> {
    when (this) {
        is IntLiteral      -> {
        }
        is ValueOf         -> if (name !in env) collect.add(name)
        is Lambda          -> body.freeVariables(env + parameters, collect)
        is Apply           -> {
            function.freeVariables(env, collect)
            arguments.flatMap { arg -> arg.freeVariables(env, collect) }
        }
        is Let             -> {
            val env1 = env + bindings.mapTo(mutableSetOf<String>()) { it.name }
            bindings.forEach { (_, v) -> v.freeVariables(env1, collect) }
            body.freeVariables(env1, collect)
        }
        is If              -> {
            cond.freeVariables(env, collect)
            then.freeVariables(env, collect)
            otherwise.freeVariables(env, collect)
        }
        is ConstructorCall -> arguments.forEach { it.freeVariables(env, collect) }
        is Match           -> arms.entries.flatMap { (p, e) -> e.freeVariables(env + p.boundVariables(), collect) }
        is ApplyBuiltin    -> arguments.forEach { it.freeVariables(env, collect) }
    }
    return collect
}

fun Expr.inferType(
    env: Env,
    namer: Namer,
    constraints: MutableList<Constraint>,
    constructorADTMap: Map<ADTConstructor, ADT>
): Type = when (this) {
    is IntLiteral      -> INT
    is ValueOf         -> env[name]?.instantiate(namer) ?: error("Unbound identifier $name")
    is Lambda          -> {
        val tyVars = List(parameters.size) { Var(namer.freshName()) }
        val newEnv = env + parameters.zip(tyVars.map { v -> TypeScheme(emptyList(), v) })
        val bodyT = body.inferType(newEnv, namer, constraints, constructorADTMap)
        functionType(tyVars, bodyT)
    }
    is Apply           -> {
        val f = function.inferType(env, namer, constraints, constructorADTMap)
        val args = arguments.map { it.inferType(env, namer, constraints, constructorADTMap) }
        val ret = Var(namer.freshName())
        constraints.bind(f, functionType(args, ret))
        ret
    }
    is Let             -> {
        val graph = makeGraph(env.keys)
        val env2 = env.toMutableMap()
        for (comp in graph.condense().topologicalSort()) {
            val typeVars = comp.associate { i -> bindings[i].name to Var(namer.freshName()) }
            val env1 = env2 + typeVars.mapValues { (_, t) -> TypeScheme(emptyList(), t) }
            val c = mutableListOf<Constraint>()
            val t = mutableMapOf<String, Type>()
            for (i in comp) {
                val (name, value) = bindings[i]
                val defTypeVar = typeVars.getValue(name)
                val type = value.inferType(env1, namer, c, constructorADTMap)
                c.bind(defTypeVar, type)
                t[name] = type
            }
            constraints.addAll(c)
            for ((name, type) in t) {
                val principal = principalType(type, env1, c)
                env2[name] = principal
            }
        }
        body.inferType(env2, namer, constraints, constructorADTMap)
    }
    is If              -> {
        val ret = Var(namer.freshName())
        val condT = cond.inferType(env, namer, constraints, constructorADTMap)
        constraints.bind(condT, Builtin.BooleanT)
        val t1 = then.inferType(env, namer, constraints, constructorADTMap)
        constraints.bind(t1, ret)
        val t2 = otherwise.inferType(env, namer, constraints, constructorADTMap)
        constraints.bind(t2, ret)
        ret
    }
    is ConstructorCall -> {
        val typeArguments = mutableMapOf<String, Type>()
        constructor.parameters.zip(arguments) { p, a ->
            if (p is Var) {
                val fresh = typeArguments.getOrPut(p.name) { Var(namer.freshName()) }
                constraints.add(Constraint(a.inferType(env, namer, constraints, constructorADTMap), fresh))
            } else {
                constraints.add(Constraint(a.inferType(env, namer, constraints, constructorADTMap), p))
            }
        }
        val adt = constructorADTMap[constructor] ?: error("No ADT found for constructor $constructor")
        val positionalTypeArgs = adt.typeParameters.map {
            typeArguments.getOrElse(it) {
                Var(namer.freshName())
            }
        }
        ParameterizedADT(adt.name, positionalTypeArgs)
    }
    is Match           -> {
        val matchedType = expr.inferType(env, namer, constraints, constructorADTMap)
        val common = Var(namer.freshName())
        for ((pattern, body) in arms) {
            val expectedType = pattern.inferExpectedType(namer, constructorADTMap)
            val returnType = body.inferType(env + pattern.defineVars(namer), namer, constraints, constructorADTMap)
            constraints.add(Constraint(matchedType, expectedType))
            constraints.add(Constraint(common, returnType))
        }
        common
    }
    is ApplyBuiltin    -> {
        parameters.zip(arguments) { p, a ->
            constraints.add(Constraint(a.inferType(env, namer, constraints, constructorADTMap), p))
        }
        returnType
    }
    is Expr.Hole       -> Type.Hole
}

fun Let.makeGraph(boundVars: Set<String>): List<MutableList<Int>> {
    val vertices = bindings.withIndex().associate { (idx, b) -> b.name to idx }
    val graph = List(bindings.size) { mutableListOf<Int>() }
    for ((i, b) in bindings.withIndex()) {
        for (v in b.value.freeVariables(boundVars)) {
            val j = vertices[v] ?: continue
            graph[j].add(i)
        }
    }
    return graph
}

fun functionType(parameters: List<Type>, returnType: Type) =
    parameters.foldRight(returnType) { t, acc -> Func(t, acc) }

fun unify(constraints: Constraints): Subst {
    if (constraints.isEmpty()) return emptyMap()
    val (l, r) = constraints.first()
    val rest = constraints.drop(1)
    return when {
        l == r || l is Type.Hole || r is Type.Hole                       -> unify(rest)
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
    return type.apply(subst).generalize(env.keys)
}

fun inferType(expr: Expr, adtDefs: List<ADTDef> = Builtin.adtDefinitions): TypeScheme {
    val constructorADTMap = adtDefs.flatMap { (adt, constructors) -> constructors.map { it to adt } }.toMap()
    val env = Builtin.env
    val namer = SimpleNamer()
    val constraints = LinkedList<Constraint>()
    val general = expr.inferType(env, namer, constraints, constructorADTMap)
    val subst = unify(constraints)
    val specialized = general.apply(subst)
    return specialized.generalize(env.keys)
}