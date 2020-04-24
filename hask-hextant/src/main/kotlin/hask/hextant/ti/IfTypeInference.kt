package hask.hextant.ti

import hask.hextant.ti.Builtins.BoolT
import hask.core.type.Type.Var
import hask.hextant.ti.unify.ConstraintsHolder
import hask.hextant.ti.unify.bind
import hask.hextant.ti.env.TIContext
import hextant.ok
import reaktive.value.*

class IfTypeInference(
    context: TIContext,
    private val condition: TypeInference,
    private val then: TypeInference,
    private val otherwise: TypeInference,
    holder: ConstraintsHolder
) : AbstractTypeInference(context, holder) {
    private val resultType = Var(context.namer.freshName())

    override val type = reactiveValue(ok(resultType))

    private val conditionObs = holder.bind(condition.type, reactiveValue(ok(BoolT)), this)

    private val thenObs = holder.bind(then.type, reactiveValue(ok(resultType)), this)

    private val otherwiseObs = holder.bind(otherwise.type, reactiveValue(ok(resultType)), this)

    override fun dispose() {
        super.dispose()
        context.namer.release(resultType.name)
        conditionObs.kill()
        thenObs.kill()
        otherwiseObs.kill()
        condition.dispose()
        then.dispose()
        otherwise.dispose()
    }
}