/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti.env

import bundles.Property
import hask.core.type.Type
import hask.core.type.TypeScheme
import hask.hextant.context.HaskInternal
import hask.hextant.ti.unify.SimpleUnificator
import hask.hextant.ti.unify.Unificator

data class TIContext(
    val namer: ReleasableNamer,
    val unificator: Unificator,
    val env: TIEnv
) {
    fun child() = copy(env = TIEnv(env, namer))

    fun displayType(type: Type) = type.apply(unificator.substitutions()).toString()

    fun displayTypeScheme(type: TypeScheme) = type.apply(unificator.substitutions()).toString()

    companion object : Property<TIContext, HaskInternal, HaskInternal>("Type Inference Context") {
        fun root(): TIContext {
            val namer = ReleasableNamer()
            val env = TIEnv(namer)
            val unifier = SimpleUnificator()
            return TIContext(namer, unifier, env)
        }
    }
}