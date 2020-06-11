/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti.unify

import hask.core.type.Type
import hask.core.type.Type.*
import kollektion.Counter
import reaktive.dependencies
import reaktive.value.*
import reaktive.value.binding.binding

class SimpleUnificator(private val parent: SimpleUnificator? = null) : Unificator {
    private val constraints = Counter<Constraint>()
    private val subst = mutableMapOf<String, ReactiveVariable<Type>>()
    private var locked = false

    private fun executeSafely(action: () -> Unit) {
        check(!locked) { "Locked" }
        locked = true
        action()
        locked = false
    }

    override fun add(constraint: Constraint) {
        parent?.add(constraint)
        executeSafely {
            println("add $constraint")
            if (constraints.add(constraint)) {
                unify(constraint.a, constraint.b, constraint)
            } else println("does not change")
        }
    }

    private fun unify(a: Type, b: Type, c: Constraint) {
        val t1 = subst(a)
        val t2 = subst(b)
        when {
            t1 == t2 || t1 == Hole || t2 == Hole           -> {
            }
            t1 is Var && t1.name !in t2.fvs()              -> bind(t1.name, t2)
            t2 is Var && t2.name !in t1.fvs()              -> bind(t2.name, t1)
            t1 is Func && t2 is Func                       -> {
                unify(t1.from, t2.from, c)
                unify(subst(t1.to), subst(t2.to), c)
            }
            t1 is ParameterizedADT && t2 is ParameterizedADT -> {
                if (t1.adt != t2.adt) c.display.reportError(a, b)
                for ((ta1, ta2) in t1.typeArguments.zip(t2.typeArguments)) {
                    unify(subst(ta1), subst(ta2), c)
                }
            }
            else                                           -> c.display.reportError(t1, t2)
        }
    }

    private fun bind(name: String, type: Type) {
        subst(name).set(type)
        for (t in subst.values) {
            val s = t.now.subst(name, type)
            t.set(s)
        }
    }

    private fun subst(name: String) = subst.getOrPut(name) { reactiveVariable(Var(name)) }

    private fun subst(t: Type) = t.apply(substitutions())

    override fun removeAll(cs: Collection<Constraint>) {
        parent?.removeAll(cs)
        executeSafely {
            println("removing $cs")
            if (constraints.removeAll(cs)) {
                for (c in constraints.asSet()) c.display.clearErrors()
                for (c in cs) c.display.clearErrors()
                subst.entries.forEach { (name, t) ->
                    t.set(Var(name))
                }
                for (c in constraints.asSet()) {
                    unify(c.a, c.b, c)
                }
            } else println("does not change")
        }
    }

    override fun remove(constraint: Constraint) {
        removeAll(listOf(constraint))
    }

    override fun substitutions(): Map<String, Type> =
        subst.mapValues { (_, s) -> s.now }.filter { (n, s) -> s != Var(n) }

    override fun substitute(type: Type): ReactiveValue<Type> = when (type) {
        INT, Hole           -> reactiveValue(type)
        is Var              -> subst(type.name)
        is Func             -> binding(substitute(type.from), substitute(type.to)) { a, b -> Func(a, b) }
        is ParameterizedADT -> {
            val args = type.typeArguments.map { substitute(it) }
            binding<Type>(dependencies(args)) { ParameterizedADT(type.adt, args.map { it.now }) }
        }
    }

    override fun constraints(): Set<Constraint> = constraints.asSet()

    override fun child(): Unificator = SimpleUnificator(this)

    override fun root(): Unificator = parent?.root() ?: this
}