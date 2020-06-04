/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti

import hask.core.type.Type
import hask.core.type.Type.Hole
import hask.hextant.ti.env.TIContext
import reaktive.value.*
import reaktive.value.binding.flatMap

class ExpanderTypeInference(
    context: TIContext,
    private val inference: ReactiveValue<TypeInference?>
) : AbstractTypeInference(context) {
    override val type: ReactiveValue<Type> = inference.flatMap { inf -> inf?.type ?: reactiveValue(Hole) }

    override fun children(): Collection<TypeInference> = inference.now?.let { listOf(it) } ?: emptyList()
}