/**
 * @author Nikolaus Knop
 */

package hask.hextant.ti.unify

import hask.core.type.Type
import hask.hextant.ti.*
import hextant.CompileResult
import hextant.ifOk
import reaktive.Observer
import reaktive.value.ReactiveValue
import reaktive.value.now

fun ConstraintsHolder.bind(
    a: ReactiveValue<CompileResult<Type>>,
    b: ReactiveValue<CompileResult<Type>>,
    ti: TypeInference
): Observer {
    fun bind(a: CompileResult<Type>, b: CompileResult<Type>) {
        ifOk(a, b) { t, s -> addConstraint(Constraint(t, s, ti)) }
    }
    fun unbind(a: CompileResult<Type>, b: CompileResult<Type>) {
        ifOk(a, b) { t, s -> removeConstraint(Constraint(t, s, ti)) }
    }
    bind(a.now, b.now)
    val aObs = a.observe { _, old, new ->
        unbind(old, b.now)
        bind(new, b.now)
    }
    val bObs = b.observe { _, old, new ->
        unbind(a.now, old)
        bind(a.now, new)
    }
    return aObs and bObs
}