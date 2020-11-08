/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti.env

import bundles.PublicProperty
import bundles.property
import hask.core.type.Type
import hask.core.type.TypeScheme
import hask.hextant.ti.unify.SimpleUnificator
import hask.hextant.ti.unify.Unificator

data class TIContext(
    val namer: ReleasableNamer,
    val unificator: Unificator,
    val env: TIEnv
) {
    fun child() = copy(env = TIEnv(env))

    fun displayType(type: Type) = type.apply(unificator.root().substitutions()).toString()

    fun displayTypeScheme(type: TypeScheme) = type.apply(unificator.root().substitutions()).toString()

    internal companion object : PublicProperty<TIContext> by property("Type Inference Context") {
        fun root(): TIContext {
            val namer = ReleasableNamer()
            val env = TIEnv()
            val unifier = SimpleUnificator()
            return TIContext(namer, unifier, env)
        }
    }
}