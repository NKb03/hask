package hask.hextant.editor

import hask.core.ast.Pattern
import hask.core.type.Type
import hask.hextant.ti.*
import hask.hextant.ti.env.TIContext
import reaktive.collection.ReactiveCollection
import reaktive.value.ReactiveValue
import validated.Validated

class MatchTypeInference(
    context: TIContext,
    matched: TypeInference,
    cases: ReactiveCollection<Pair<ReactiveValue<Validated<Pattern>>, ExpanderTypeInference>>
) : AbstractTypeInference(context) {

    override val type: ReactiveValue<Type> = matched.type
}
