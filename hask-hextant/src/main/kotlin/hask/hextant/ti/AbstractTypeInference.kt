/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti

import hask.core.type.Type
import hask.hextant.ti.unify.ConstraintsHolder
import hask.hextant.ti.env.TIContext
import hask.hextant.ti.impl.Counter
import reaktive.set.ReactiveSet
import reaktive.set.reactiveSet

abstract class AbstractTypeInference(
    final override val context: TIContext,
    protected val holder: ConstraintsHolder
) : TypeInference {
    protected var disposed = false
        private set

    override fun dispose() {
        check(!disposed) { "$this is already disposed" }
        disposed = true
        holder.clearConstraints()
    }

    private val errSet = reactiveSet<Pair<Type, Type>>()

    private val _errors = Counter(errSet.now)

    final override val errors: ReactiveSet<Pair<Type, Type>> get() = errSet

    final override fun reportError(a: Type, b: Type) {
        _errors.add(a to b)
    }

    override fun removeError(a: Type, b: Type) {
        _errors.remove(a to b)
    }

    override fun clearErrors() {
        _errors.clear()
    }
}