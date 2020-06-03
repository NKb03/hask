/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti.unify

import hask.core.type.Type
import hask.core.type.Type.*
import reaktive.dependencies
import reaktive.value.*
import reaktive.value.binding.binding

class GroupedUnificator(private val parent: GroupedUnificator? = null) : Unificator {
    private val adj = mutableMapOf<String, MutableSet<String>>()
    private val constraints = mutableMapOf<String, MutableSet<Constraint>>()
    private val subst = mutableMapOf<String, ReactiveVariable<Type>>()
    private val types = mutableMapOf<String, MutableSet<ReactiveVariable<Type>>>()

    override fun add(constraint: Constraint) {
        //        println("adding $constraint")
        unify(constraint.a, constraint.b, constraint)
        parent?.add(constraint)
    }

    private fun dfs(u: String, visited: MutableSet<String>) {
        visited.add(u)
        for (v in adj(u)) {
            if (v !in visited) dfs(v, visited)
        }
    }

    override fun remove(constraint: Constraint) {
        //        println("removing $constraint")
        val vars = mutableSetOf<String>()
        val c = mutableSetOf<Constraint>()
        for (v in constraint.fvs) dfs(v, vars)
        for (v in vars) {
            subst(v).set(Var(v))
            adj(v).clear()
            c.addAll(constraints(v))
            constraints(v).clear()
            types(v).clear()
        }
        for (cstr in c) cstr.display.clearErrors()
        for (cstr in c) {
            if (cstr != constraint) add(cstr)
        }
        parent?.remove(constraint)
    }

    override fun removeAll(cs: Collection<Constraint>) {
        for (c in cs) remove(c)
        parent?.removeAll(cs)
    }

    private fun unify(a: Type, b: Type, c: Constraint) {
        val t1 = subst(a)
        val t2 = subst(b)
        for (v in t1.fvs()) constraints(v).add(c)
        for (v in t2.fvs()) constraints(v).add(c)
        when {
            t1 == t2 || t1 == Wildcard || t2 == Wildcard   -> return
            t1 is Var && t1.name !in t2.fvs()              -> bind(t1.name, t2)
            t2 is Var && t2.name !in t1.fvs()              -> bind(t2.name, t1)
            t1 is Func && t2 is Func                       -> {
                unify(t1.from, t2.from, c)
                unify(subst(t1.to), subst(t2.to), c)
            }
            a is ParameterizedADT && b is ParameterizedADT -> {
                if (a.adt != b.adt) cannotUnify(a, b, c)
                for ((ta1, ta2) in a.typeArguments.zip(b.typeArguments)) {
                    unify(subst(ta1), subst(ta2), c)
                }
            }
            else                                           -> cannotUnify(t1, t2, c)
        }
    }

    private fun subst(t: Type) = t.apply(substitutions())

    private fun cannotUnify(a: Type, b: Type, constraint: Constraint) {
        constraint.display.reportError(a, b)
    }

    private fun constraints(name: String) = constraints.getOrPut(name) { mutableSetOf() }

    private fun adj(name: String) = adj.getOrPut(name) { mutableSetOf() }

    private fun subst(name: String) = subst.getOrPut(name) { reactiveVariable(Var(name)) }

    private fun types(name: String) = types.getOrPut(name) { mutableSetOf() }

    private fun bind(name: String, type: Type) {
        adj(name).addAll(type.fvs())
        for (v in type.fvs()) {
            adj(v).add(name)
            types(v).add(subst(name))
        }
        subst(name).set(type)
        for (t in types(name)) {
            t.set(t.now.subst(name, type))
        }
    }

    override fun substitutions(): Map<String, Type> =
        subst.mapValues { (_, s) -> s.now }.filter { (n, s) -> s != Var(n) }

    override fun substitute(type: Type): ReactiveValue<Type> = when (type) {
        INT, Wildcard       -> reactiveValue(type)
        is Var              -> subst(type.name)
        is Func             -> binding(substitute(type.from), substitute(type.to)) { a, b -> Func(a, b) }
        is ParameterizedADT -> {
            val args = type.typeArguments.map { substitute(it) }
            binding<Type>(dependencies(args)) { ParameterizedADT(type.adt, args.map { it.now }) }
        }
    }

    override fun constraints(): Set<Constraint> = constraints.values.flatten().toSet()

    override fun child(): Unificator = GroupedUnificator(this)
}