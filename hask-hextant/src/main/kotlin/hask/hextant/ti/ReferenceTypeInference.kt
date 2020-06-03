/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti

import hask.core.type.Type
import hask.core.type.Type.Wildcard
import hask.hextant.ti.env.TIContext
import reaktive.value.*
import reaktive.value.binding.flatMap
import reaktive.value.binding.map
import validated.ifInvalid
import validated.reaktive.ReactiveValidated

class ReferenceTypeInference(context: TIContext, private val name: ReactiveValidated<String>) :
    AbstractTypeInference(context) {
    private val _type = reactiveVariable<Type>(Wildcard)
    private val binding by lazy {
        name.flatMap {
            val n = it.ifInvalid { return@flatMap reactiveValue(Wildcard) }
            context.env.resolve(n).map { t -> t ?: Wildcard }
        }
    }

    override val type: ReactiveValue<Type> get() = _type
    override fun onActivate() {
        addObserver(_type.bind(binding))
        addObserver(type.forEach(::releaseAndUseNames))
    }

    override fun onDeactivate() {
        binding.dispose()
    }

    private fun releaseAndUseNames(new: Type) {
        releaseAllNames()
        useNames(new.fvs(env = context.env.freeTypeVars.now))
    }
}