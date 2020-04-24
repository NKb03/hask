/**
 * @author Nikolaus Knop
 */

package hask.hextant.ti.unify

interface ConstraintsHolder {
    fun addConstraint(constraint: Constraint)

    fun addConstraints(constraints: Iterable<Constraint>)

    fun removeConstraint(constraint: Constraint)

    fun clearConstraints()
}