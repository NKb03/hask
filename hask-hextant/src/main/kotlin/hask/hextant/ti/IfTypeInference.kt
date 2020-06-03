package hask.hextant.ti

import hask.core.type.Type
import hask.core.type.Type.Var
import hask.core.type.Type.Wildcard
import hask.hextant.ti.Builtins.BoolT
import hask.hextant.ti.env.TIContext
import reaktive.value.*

class IfTypeInference(
    context: TIContext,
    private val condition: TypeInference,
    private val then: TypeInference,
    private val otherwise: TypeInference
) : AbstractTypeInference(context) {
    private val result by lazy { Var(freshName()) }

    private var _type = reactiveVariable<Type>(Wildcard)
    override val type: ReactiveValue<Type> get() = _type

    init {
        dependsOn(condition.type, then.type, otherwise.type)
    }

    override fun onActivate() {
        _type.set(result)
    }

    override fun doRecompute() {
        addConstraint(condition.type.now, BoolT)
        addConstraint(then.type.now, result)
        addConstraint(otherwise.type.now, result)
    }

    override fun children(): Collection<TypeInference> = listOf(condition, then, otherwise)
}