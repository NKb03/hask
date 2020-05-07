/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti

import hask.core.type.Type
import hask.hextant.ti.env.TIContext
import hask.hextant.ti.impl.Counter
import hask.hextant.ti.unify.Constraint
import hextant.EditorResult
import hextant.ifOk
import reaktive.*
import reaktive.set.ReactiveSet
import reaktive.set.reactiveSet
import reaktive.value.now

abstract class AbstractTypeInference(final override val context: TIContext) : TypeInference {
    private var _active = false

    override val active: Boolean
        get() = _active

    private val constraints = mutableSetOf<Constraint>()
    private val usedNames = mutableSetOf<String>()
    private val observers = mutableListOf<Observer>()
    private val shortObservers = mutableListOf<Observer>()

    private val errSet = reactiveSet<Pair<Type, Type>>()
    private val _errors = Counter(errSet.now)
    final override val errors: ReactiveSet<Pair<Type, Type>> get() = errSet

    protected open fun doReset() {}

    protected open fun doRecompute() {}

    protected open fun children(): Collection<TypeInference> = emptyList()

    final override fun reportError(a: Type, b: Type) {
        _errors.add(a to b)
    }

    override fun removeError(a: Type, b: Type) {
        _errors.remove(a to b)
    }

    override fun clearErrors() {
        _errors.clear()
    }

    protected fun freshName(): String {
        check(active) { "Cannot generate fresh name when not active" }
        val name = context.namer.freshName()
        useName(name)
        return name
    }

    protected fun useName(name: String) {
        usedNames.add(name)
    }

    protected fun useNames(names: Collection<String>) {
        usedNames.addAll(names)
    }

    protected fun releaseName(name: String) {
        check(usedNames.remove(name)) { "$name was not used" }
        context.namer.release(name)
    }

    protected fun releaseAllNames() {
        for (n in usedNames) context.namer.release(n)
        usedNames.clear()
    }

    protected fun addConstraint(a: Type, b: Type) {
        println("$this add constraint $a = $b")
        val c = Constraint(a, b, this)
        constraints.add(c)
        if (active) context.unificator.add(c)
    }

    protected fun bind(a: EditorResult<Type>, b: Type) {
        a.now.ifOk { t -> addConstraint(t, b) }
        addObserver(a.observe { _, old, new ->
            old.ifOk { t -> removeConstraint(t, b) }
            new.ifOk { t -> addConstraint(t, b) }
        }, killOnReset = true)
    }

    protected fun removeConstraint(a: Type, b: Type) {
        println("$this remove constraint $a = $b")
        val c = Constraint(a, b, this)
        constraints.remove(c)
        if (active) context.unificator.remove(c)
    }

    protected fun clearConstraints() {
        println("$this clear constraints")
        context.unificator.removeAll(constraints)
        constraints.clear()
    }

    protected fun addObserver(o: Observer, killOnReset: Boolean = false) {
        if (killOnReset) shortObservers.add(o)
        else observers.add(o)
    }

    protected fun dependsOn(dependency: Reactive) {
        val o = dependency.observe { _ -> recompute() }
        addObserver(o)
    }

    protected fun dependsOn(vararg deps: Reactive) {
        dependsOn(dependencies(*deps))
    }

    protected fun reset() {
        for (o in shortObservers) o.kill()
        shortObservers.clear()
        clearConstraints()
        releaseAllNames()
        doReset()
    }

    protected fun recompute() {
        if (active) {
            reset()
            doRecompute()
        }
    }

    override fun activate() {
        if (active) return
        actives.add(this)
        for (c in children()) c.activate()
        _active = true
        doRecompute()
    }

    override fun deactivate() {
        if (!active) return
        actives.remove(this)
        _active = false
        reset()
        for (c in children()) c.deactivate()
    }

    companion object {
        private val actives = mutableSetOf<TypeInference>()

        fun actives(): Set<TypeInference> = actives
    }
}