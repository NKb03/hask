/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti.env

import hask.hextant.context.HaskInternal
import hask.hextant.ti.unify.GroupedUnificator
import hask.hextant.ti.unify.Unificator
import hextant.bundle.Property

data class TIContext(
    val namer: ReleasableNamer,
    val unificator: Unificator,
    val env: TIEnv
) {
    fun child() = copy(env = SimpleTIEnv(env, namer))

    companion object : Property<TIContext, HaskInternal, HaskInternal>("Type Inference Context") {
        fun root(): TIContext {
            val namer = ReleasableNamer()
            val env = SimpleTIEnv(namer)
            val unifier = GroupedUnificator()
            return TIContext(namer, unifier, env)
        }
    }
}