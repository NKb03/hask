/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti.unify

class CollectingConstraintsHolder(private val dest: MutableCollection<Constraint>) :
    ConstraintsHolder {
    private val myConstraints = mutableSetOf<Constraint>()

    override fun addConstraint(constraint: Constraint) {
        myConstraints.add(constraint)
        dest.add(constraint)
    }

    override fun addConstraints(constraints: Iterable<Constraint>) {
        myConstraints.addAll(constraints)
        dest.addAll(constraints)
    }

    override fun removeConstraint(constraint: Constraint) {
        myConstraints.remove(constraint)
        dest.remove(constraint)
    }

    override fun clearConstraints() {
        dest.removeAll(myConstraints)
        myConstraints.clear()
    }
}