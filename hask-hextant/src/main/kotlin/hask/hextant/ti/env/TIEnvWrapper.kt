/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti.env

import hask.core.type.TypeScheme
import hask.core.type.Type
import hextant.CompileResult
import reaktive.set.binding.flattenToSet
import reaktive.value.*
import reaktive.value.binding.flatMap
import reaktive.value.binding.map

class TIEnvWrapper(initial: TIEnv):
    TIEnv {
    private val wrapped = reactiveVariable(initial)

    fun setWrapped(env: TIEnv) {
        wrapped.set(env)
    }

    override fun resolve(name: String): ReactiveValue<CompileResult<Type>?> = wrapped.flatMap { it.resolve(name) }

    override val now: Map<String, CompileResult<TypeScheme>>
        get() = wrapped.now.now

    override fun generalize(t: Type): ReactiveValue<TypeScheme> = wrapped.flatMap { it.generalize(t) }

    override val freeTypeVars = wrapped.map { it.freeTypeVars }.flattenToSet()
}