/**
 * @author Nikolaus Knop
 */

package hask.hextant.ti

import hask.core.type.Type
import hask.core.type.Type.*
import hask.hextant.ti.env.ErrorDisplay
import hask.hextant.ti.unify.Constraint

fun List<Constraint>.apply(subst: Map<String, Type>) =
    map { (s, t, ti) -> Constraint(s.apply(subst), t.apply(subst), ti) }

fun Map<String, Type>.compose(other: Map<String, Type>) = this + other.mapValues { it.value.apply(this) }

fun unify(constraints: List<Constraint>, display: ErrorDisplay): Map<String, Type> {
    if (constraints.isEmpty()) return emptyMap()
    val (l, r) = constraints.first()
    val rest = constraints.drop(1)
    return when {
        l == r                                                           -> unify(rest)
        l is Var && l.name !in r.fvs()                                   -> {
            val subst = mapOf(l.name to r)
            unify(rest.apply(subst)).compose(subst)
        }
        r is Var && r.name !in l.fvs()                                   -> {
            val subst = mapOf(r.name to l)
            unify(rest.apply(subst)).compose(subst)
        }
        l is Func && r is Func                                           -> {
            val additional = listOf(
                Constraint(l.from, r.from, display),
                Constraint(l.to, r.to, display)
            )
            unify(rest + additional)
        }
        l is ParameterizedADT && r is ParameterizedADT && l.adt == r.adt -> {
            val additional = l.typeArguments.zip(r.typeArguments) { s, t ->
                Constraint(
                    s,
                    t,
                    display
                )
            }
            unify(rest + additional)
        }
        else                                                             -> error("Cannot solve constraint $l = $r")
    }
}

fun unify(constraints: List<Constraint>): Map<String, Type> {
    return if (constraints.isEmpty()) emptyMap()
    else {
        val (a, b, ti) = constraints.first()
        val subst = unify(constraints.drop(1))
        unify_one(a.apply(subst), b.apply(subst), ti).compose(subst)
    }
}

fun unify_one(l: Type, r: Type, ti: ErrorDisplay): Map<String, Type> {
    return when {
        l == r                                                           -> emptyMap()
        l is Var && l.name !in r.fvs()                                   -> mapOf(l.name to r)
        r is Var && r.name !in l.fvs()                                   -> mapOf(r.name to l)
        l is Func && r is Func                                           ->
            unify(listOf(
                Constraint(l.from, r.from, ti),
                Constraint(l.to, r.to, ti)
            ))
        l is ParameterizedADT && r is ParameterizedADT && l.adt == r.adt ->
            unify(l.typeArguments.zip(r.typeArguments) { s, t ->
                Constraint(
                    s,
                    t,
                    ti
                )
            })
        else                                                             -> error("Cannot solve constraint $l = $r")
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

fun unify1(constraints: List<Constraint>): Map<String, Type> {
    val subst = mutableMapOf<String, Type>()
    for ((a, b) in constraints) {
        unify(a.apply(subst), b.apply(subst), subst)
    }
    return subst
}