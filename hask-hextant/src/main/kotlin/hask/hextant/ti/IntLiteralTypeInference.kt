package hask.hextant.ti

import hask.core.type.Type.INT
import hask.hextant.ti.unify.ConstraintsHolder
import hask.hextant.ti.env.TIContext
import hextant.ok
import reaktive.value.reactiveValue

class IntLiteralTypeInference(context: TIContext, holder: ConstraintsHolder) : AbstractTypeInference(context, holder) {
    override val type = reactiveValue(ok(INT))
}