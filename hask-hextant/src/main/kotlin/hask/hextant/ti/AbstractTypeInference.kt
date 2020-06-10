/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti

import hask.core.type.Namer
import hask.core.type.Type
import hask.core.type.Type.Var
import hask.hextant.ti.env.TIContext
import hask.hextant.ti.unify.Constraint
import kollektion.Counter
import reaktive.*
import reaktive.set.ReactiveSet
import reaktive.set.reactiveSet
import reaktive.value.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class AbstractTypeInference(final override val context: TIContext) : TypeInference, Namer {
    private var activated = false
    private var disposed = false
    final override val isActive: Boolean get() = activated && !disposed

    private val _errors = reactiveSet<Pair<Type, Type>>()
    final override val errors: ReactiveSet<Pair<Type, Type>> get() = _errors

    private val constraints = Counter<Constraint>()
    private val usedNames = mutableSetOf<String>()
    private val longTermNames = mutableSetOf<String>()
    private val observers = mutableSetOf<Observer>()
    private val shortTermObservers = mutableSetOf<Observer>()
    private val delegates = mutableListOf<Delegate<Any>>()

    protected open fun onActivate() {}

    protected open fun doRecompute() {}

    protected open fun onDeactivate() {}

    protected open fun onReset() {}

    protected open fun children(): Collection<TypeInference> = emptyList()

    protected fun addConstraint(c: Constraint) {
        ensureActive()
        if (constraints.add(c)) context.unificator.add(c)
    }

    protected fun addConstraint(a: Type, b: Type) {
        val c = Constraint(a, b, this)
        addConstraint(c)
    }

    protected fun removeConstraint(c: Constraint) {
        ensureActive()
        if (constraints.remove(c)) context.unificator.remove(c)
    }

    protected fun removeConstraint(a: Type, b: Type) {
        val c = Constraint(a, b, this)
        removeConstraint(c)
    }

    protected fun clearConstraints() {
        ensureActive()
        context.unificator.removeAll(constraints.asSet())
        constraints.clear()
    }

    protected fun typeVariable(): ReadOnlyProperty<AbstractTypeInference, Var> = active { Var(longTermFreshName()) }

    protected fun <T : Any> active(initializer: () -> T): ReadOnlyProperty<AbstractTypeInference, T> {
        check(!disposed) { "Already disposed" }
        val d = Delegate(initializer)
        if (isActive) d.activate()
        else delegates.add(d)
        return d
    }

    protected fun longTermFreshName(): String {
        ensureActive()
        val n = context.namer.freshName()
        longTermNames.add(n)
        return n
    }

    override fun freshName(): String {
        ensureActive()
        val n = context.namer.freshName()
        useName(n)
        return n
    }

    protected fun useName(name: String) {
        ensureActive()
        usedNames.add(name)
    }

    protected fun useNames(names: Collection<String>) {
        ensureActive()
        usedNames.addAll(names)
    }

    protected fun releaseName(name: String) {
        ensureActive()
        check(usedNames.remove(name)) { "$name was not used" }
        context.namer.release(name)
    }

    protected fun releaseAllNames() {
        ensureActive()
        for (n in usedNames) context.namer.release(n)
        usedNames.clear()
    }

    protected fun addObserver(o: Observer, killOnRecompute: Boolean = false) {
        if (killOnRecompute) shortTermObservers.add(o)
        else observers.add(o)
    }

    protected fun dependsOn(dependency: Reactive) {
        addObserver(dependency.observe { if (isActive) recompute() })
    }

    protected fun dependsOn(vararg deps: Reactive) {
        dependsOn(dependencies(*deps))
    }

    protected fun bind(a: ReactiveValue<Type>, b: Type) {
        addConstraint(a.now, b)
        val o = a.observe { old, new ->
            removeConstraint(old, b)
            addConstraint(new, b)
        }
        addObserver(o, killOnRecompute = true)
    }

    private fun ensureActive() {
        check(activated) { "Not activated" }
        check(!disposed) { "Already disposed" }
    }

    protected fun reset() {
        ensureActive()
        clearConstraints()
        releaseAllNames()
        shortTermObservers.forEach { o -> o.kill() }
        shortTermObservers.clear()
        onReset()
    }

    protected fun recompute() {
        ensureActive()
        reset()
        doRecompute()
    }

    final override fun activate() {
        check(!activated) { "Already activated" }
        activated = true
        for (d in delegates) d.activate()
        children().forEach { c -> if (!c.isActive) c.activate() }
        onActivate()
        doRecompute()
    }

    final override fun dispose() {
        ensureActive()
        observers.forEach { it.kill() }
        observers.clear()
        delegates.clear()
        reset()
        onDeactivate()
        disposed = true
        children().forEach { c -> c.dispose() }
    }

    final override fun reportError(a: Type, b: Type) {
        _errors.now.add(a to b)
    }

    final override fun removeError(a: Type, b: Type) {
        _errors.now.remove(a to b)
    }

    final override fun clearErrors() {
        _errors.now.clear()
    }

    private class Delegate<out T : Any>(initializer: () -> T) : ReadOnlyProperty<AbstractTypeInference, T> {
        private lateinit var value: T
        private var initializer: (() -> T)? = initializer

        fun activate() {
            value = initializer?.invoke() ?: error("Already activated")
            initializer = null
        }

        override fun getValue(thisRef: AbstractTypeInference, property: KProperty<*>): T = value
    }
}