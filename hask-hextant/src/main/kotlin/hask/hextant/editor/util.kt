/**
 * @author Nikolaus Knop
 */

package hask.hextant.editor

import hask.hextant.context.HaskInternal
import hask.hextant.ti.env.TIContext
import hask.hextant.ti.unify.ConstraintsHolderFactory
import hextant.Context

fun Context.withChildTIContext(): Context {
    return Context.newInstance(this) {
        set(HaskInternal, TIContext, get(HaskInternal, TIContext).child())
    }
}

fun Context.createConstraintsHolder() = get(HaskInternal, ConstraintsHolderFactory).createHolder()