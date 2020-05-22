/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti

import hask.core.type.Type
import hask.hextant.ti.env.TIContext
import hextant.childErr
import hextant.or
import reaktive.value.*
import reaktive.value.binding.flatMap
import reaktive.value.binding.map

class ExpanderTypeInference(
    context: TIContext,
    private val inference: ReactiveValue<TypeInference?>
) : AbstractTypeInference(context) {
    override fun children(): Collection<TypeInference> = inference.now?.let { listOf(it) } ?: emptyList()

    override val type = inference.flatMap { inf ->
        inf?.type?.map { t -> t.or(childErr()) } ?: reactiveValue(childErr<Type>())
    }
}