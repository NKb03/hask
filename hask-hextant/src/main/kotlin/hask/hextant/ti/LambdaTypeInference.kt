/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti

import hask.core.type.TypeScheme
import hask.core.type.Type
import hask.hextant.ti.unify.ConstraintsHolder
import hask.hextant.ti.env.SimpleTIEnv
import hask.hextant.ti.env.TIContext
import hextant.*
import reaktive.value.binding.map
import reaktive.value.now

class LambdaTypeInference(
    context: TIContext,
    parameterName: EditorResult<String>,
    private val bodyTypeInference: TypeInference,
    holder: ConstraintsHolder
) : AbstractTypeInference(context, holder) {
    private val bodyEnv get() = bodyTypeInference.context.env as SimpleTIEnv

    private val parameterTypeName = context.namer.freshName()

    val parameterType = Type.Var(parameterTypeName)

    private val parameterScheme = TypeScheme(emptyList(), parameterType)

    init {
        parameterName.now.map { bodyEnv.bind(it, ok(parameterScheme)) }
    }

    private val parameterObs = parameterName.observe { _, old, new ->
        old.map { bodyEnv.unbind(it) }
        new.map { bodyEnv.bind(it, ok(parameterScheme)) }
    }

    override fun dispose() {
        super.dispose()
        parameterObs.kill()
        context.namer.release(parameterTypeName)
        bodyTypeInference.dispose()
    }

    override val type = bodyTypeInference.type
        .map { resultType -> resultType.or(childErr())
            .map { t -> Type.Func(parameterType, t) } }
}