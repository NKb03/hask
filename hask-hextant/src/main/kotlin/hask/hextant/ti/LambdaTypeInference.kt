/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti

import hask.core.type.Type
import hask.core.type.Type.Var
import hask.core.type.functionType
import hask.hextant.ti.env.TIContext
import hextant.*
import reaktive.dependencies
import reaktive.event.unitEvent
import reaktive.list.ReactiveList
import reaktive.set.asSet
import reaktive.value.binding.binding
import reaktive.value.now

class LambdaTypeInference(
    context: TIContext,
    private val parameters: ReactiveList<CompileResult<String>>,
    private val body: TypeInference
) : NewAbstractTypeInference(context) {
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
            p.ifOk { name -> bodyEnv.bind(name, v) }
        }
    }

    override fun children(): Collection<TypeInference> = listOf(body)

    override val type = binding<CompileResult<Type>>(dependencies(body.type, parametersChange.stream)) {
        body.type.now.map { t -> functionType(typeVars, t) }
    }
}