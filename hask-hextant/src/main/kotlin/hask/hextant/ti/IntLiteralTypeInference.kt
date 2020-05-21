package hask.hextant.ti

import hask.core.type.Type.INT
import hask.hextant.ti.env.TIContext
import hextant.ok
import reaktive.value.reactiveValue

class IntLiteralTypeInference(context: TIContext) : NewAbstractTypeInference(context) {
    override val type = reactiveValue(ok(INT))
}