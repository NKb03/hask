package hask.hextant.ti

import hask.core.type.Type
import hask.core.type.Type.INT
import hask.hextant.ti.env.TIContext
import reaktive.value.ReactiveValue
import reaktive.value.reactiveValue

class IntLiteralTypeInference(context: TIContext) : AbstractTypeInference(context) {
    override val type: ReactiveValue<Type> = reactiveValue(INT)
}