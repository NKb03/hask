/**
 * @author Nikolaus Knop
 */

package hask.hextant.ti.unify

import hask.hextant.context.HaskInternal
import hextant.bundle.Property

interface ConstraintsHolderFactory {
    fun createHolder(): ConstraintsHolder

    private class Collecting(private val set: MutableCollection<Constraint>):
        ConstraintsHolderFactory {
        override fun createHolder(): ConstraintsHolder =
            CollectingConstraintsHolder(set)
    }

    private class Unifying(private val unificator: Unificator) :
        ConstraintsHolderFactory {
        override fun createHolder(): ConstraintsHolder =
            UnifyingConstraintsHolder(unificator)
    }

    companion object: Property<ConstraintsHolderFactory, HaskInternal, HaskInternal>("constraints holder factory") {
        fun collecting(dest: MutableCollection<Constraint>): ConstraintsHolderFactory =
            Collecting(dest)

        fun unifying(unificator: Unificator): ConstraintsHolderFactory =
            Unifying(unificator)
    }
}