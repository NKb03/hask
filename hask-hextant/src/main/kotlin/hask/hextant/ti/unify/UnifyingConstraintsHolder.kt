/**
 * @author Nikolaus Knop
 */

package hask.hextant.ti.unify

class UnifyingConstraintsHolder(private val unificator: Unificator): AbstractConstraintsHolder() {
    override fun doAdd(constraint: Constraint) {
        unificator.add(constraint)
    }

    override fun doRemove(constraint: Constraint) {
        unificator.remove(constraint)
    }
}