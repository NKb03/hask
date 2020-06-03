/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti

import hask.core.type.Type
import hask.core.type.Type.Var
import hask.core.type.functionType
import hask.hextant.ti.env.TIContext
import reaktive.dependencies
import reaktive.event.unitEvent
import reaktive.list.ReactiveList
import reaktive.set.asSet
import reaktive.value.ReactiveValue
import reaktive.value.binding.binding
import reaktive.value.now
import validated.Validated
import validated.ifValid

class LambdaTypeInference(
    context: TIContext,
    private val parameters: ReactiveList<Validated<String>>,
    private val body: TypeInference
) : AbstractTypeInference(context) {
    private val bodyEnv get() = body.context.env
    val typeVars = mutableListOf<Type>()
    private val parametersChange = unitEvent()

    init {
        dependsOn(parameters.asSet())
    }

    override fun onReset() {
        bodyEnv.clear()
        typeVars.clear()
    }

    override fun doRecompute() {
        for (p in parameters.now) {
            val v = Var(freshName())
            typeVars.add(v)
            p.ifValid { name -> bodyEnv.bind(name, v) }
        }
        parametersChange.fire()
    }

    override fun children(): Collection<TypeInference> = listOf(body)

    override val type: ReactiveValue<Type> = binding<Type>(dependencies(body.type, parametersChange.stream)) {
        functionType(typeVars, body.type.now)
    }
}