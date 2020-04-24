/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti.unify

abstract class AbstractConstraintsHolder : ConstraintsHolder {
    private val myConstraints = mutableSetOf<Constraint>()

    protected abstract fun doAdd(constraint: Constraint)
    protected abstract fun doRemove(constraint: Constraint)

    override fun addConstraint(constraint: Constraint) {
        if (myConstraints.add(constraint)) doAdd(constraint)
    }

    override fun addConstraints(constraints: Iterable<Constraint>) {
        for (c in constraints) addConstraint(c)
    }

    override fun removeConstraint(constraint: Constraint) {
        if (myConstraints.remove(constraint)) doRemove(constraint)
    }

    override fun clearConstraints() {
        for (c in myConstraints) doRemove(c)
        myConstraints.clear()
    }
}