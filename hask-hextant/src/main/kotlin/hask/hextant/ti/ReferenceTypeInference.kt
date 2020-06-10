/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti

import hask.core.type.Type
import hask.core.type.Type.Hole
import hask.hextant.ti.env.TIContext
import reaktive.value.*
import reaktive.value.binding.flatMap
import reaktive.value.binding.map
import validated.ifInvalid
import validated.reaktive.ReactiveValidated

class ReferenceTypeInference(context: TIContext, private val name: ReactiveValidated<String>) :
    AbstractTypeInference(context) {
    private val _type = reactiveVariable<Type>(Hole)

    private val binding by active {
        name.flatMap {
            releaseAllNames()
            val n = it.ifInvalid { return@flatMap reactiveValue(Hole) }
            context.env.resolve(n, this).map { t -> t ?: Hole }
        }
    }

    override val type: ReactiveValue<Type> get() = _type

    override fun onActivate() {
        addObserver(_type.bind(binding))
    }

    override fun onDeactivate() {
        binding.dispose()
    }
}