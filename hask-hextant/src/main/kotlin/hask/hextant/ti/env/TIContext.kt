/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti.env

import bundles.Property
import hask.core.type.Type
import hask.core.type.TypeScheme
import hask.hextant.context.HaskInternal
import hask.hextant.ti.unify.*
import hextant.*
data class TIContext(
    val namer: ReleasableNamer,
    val unificator: Unificator,
    val env: TIEnv
) {
    fun child() = copy(env = TIEnv(env, namer))

    fun displayType(type: CompileResult<Type>) =  when (type) {
        is Err   -> "[ERROR: ${type.message}]"
        is Ok    -> type.value.apply(unificator.substitutions()).toString()
        ChildErr -> "[ERROR]"
    }

    fun displayTypeScheme(type: CompileResult<TypeScheme>) =  when (type) {
        is Err   -> "[ERROR: ${type.message}]"
        is Ok    -> type.value.apply(unificator.substitutions()).toString()
        ChildErr -> "[ERROR]"
    }

    companion object : Property<TIContext, HaskInternal, HaskInternal>("Type Inference Context") {
        fun root(): TIContext {
            val namer = ReleasableNamer()
            val env = TIEnv(namer)
            val unifier = SimpleUnificator()
            return TIContext(namer, unifier, env)
        }
    }
}