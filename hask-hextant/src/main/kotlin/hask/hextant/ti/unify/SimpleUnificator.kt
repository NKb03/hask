/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti.unify

import hask.core.type.Type
import hask.core.type.Type.*
import reaktive.dependencies
import reaktive.value.*
import reaktive.value.binding.binding

class SimpleUnificator : Unificator {
    private val constraints = mutableSetOf<Constraint>()
    private val subst = mutableMapOf<String, ReactiveVariable<Type>>()

    override fun add(constraint: Constraint) {
        constraints.add(constraint)
        unify(constraint.a, constraint.b, constraint)
    }

    private fun unify(a: Type, b: Type, c: Constraint) {
        val t1 = subst(a)
        val t2 = subst(b)
        constraints.add(c)
        when {
            t1 == t2                                       -> {
            }
            t1 is Var && t1.name !in t2.fvs()              -> bind(t1.name, t2)
            t2 is Var && t2.name !in t1.fvs()              -> bind(t2.name, t1)
            t1 is Func && t2 is Func                       -> {
                unify(t1.from, t2.from, c)
                unify(subst(t1.to), subst(t2.to), c)
            }
            a is ParameterizedADT && b is ParameterizedADT -> {
                if (a.adt != b.adt) c.display.reportError(a, b)
                for ((ta1, ta2) in a.typeArguments.zip(b.typeArguments)) {
                    unify(subst(ta1), subst(ta2), c)
                }
            }
            else                                           -> c.display.reportError(t1, t2)
        }
    }

    private fun bind(name: String, type: Type) {
        subst(name).set(type)
        for (t in subst.values) t.set(t.now.subst(name, type))
    }

    private fun subst(name: String) = subst.getOrPut(name) { reactiveVariable(Var(name)) }

    private fun subst(t: Type) = t.apply(substitutions())

    override fun removeAll(cs: Collection<Constraint>) {
        if (cs.isEmpty()) return
        for (c in constraints) c.display.clearErrors()
        constraints.removeAll(cs)
        subst.clear()
        for (c in constraints) unify(c.a, c.b, c)
    }

    override fun remove(constraint: Constraint) {
        removeAll(listOf(constraint))
    }

    override fun substitutions(): Map<String, Type> =
        subst.mapValues { (_, s) -> s.now }.filter { (n, s) -> s != Var(n) }

    override fun substitute(type: Type): ReactiveValue<Type> = when (type) {
        INT                 -> reactiveValue(INT)
        is Var              -> subst(type.name)
        is Func             -> binding(substitute(type.from), substitute(type.to)) { a, b -> Func(a, b) }
        is ParameterizedADT -> {
            val args = type.typeArguments.map { substitute(it) }
            binding<Type>(dependencies(args)) { ParameterizedADT(type.adt, args.map { it.now }) }
        }
    }

    override fun constraints(): Set<Constraint> = constraints
}