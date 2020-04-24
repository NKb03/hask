/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti.unify

import hask.core.type.Type
import hask.hextant.ti.env.ErrorDisplay

data class Constraint(val a: Type, val b: Type, val display: ErrorDisplay){
    override fun toString(): String = "$a = $b"

    val fvs = a.fvs() + b.fvs()
}