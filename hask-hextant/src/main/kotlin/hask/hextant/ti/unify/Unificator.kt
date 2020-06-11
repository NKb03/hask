/**
 * @author Nikolaus Knop
 */

package hask.hextant.ti.unify

import hask.core.type.Type
import reaktive.value.ReactiveValue

interface Unificator {
    fun add(constraint: Constraint)

    fun remove(constraint: Constraint)

    fun removeAll(cs: Collection<Constraint>)

    fun substitutions(): Map<String, Type>

    fun substitute(type: Type): ReactiveValue<Type>

    fun substituteNow(type: Type): Type = type.apply(substitutions())

    fun constraints(): Set<Constraint>

    fun child(): Unificator

    fun root(): Unificator
}