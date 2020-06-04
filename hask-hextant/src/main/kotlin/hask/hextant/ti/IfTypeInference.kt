package hask.hextant.ti

import hask.core.ast.Builtin.Companion.BooleanT
import hask.core.type.Type
import hask.core.type.Type.Var
import hask.core.type.Type.Hole
import hask.hextant.ti.env.TIContext
import reaktive.value.*

class IfTypeInference(
    context: TIContext,
    private val condition: TypeInference,
    private val then: TypeInference,
    private val otherwise: TypeInference
) : AbstractTypeInference(context) {
    private val result by lazy { Var(freshName()) }

    private var _type = reactiveVariable<Type>(Hole)
    override val type: ReactiveValue<Type> get() = _type

    init {
        dependsOn(condition.type, then.type, otherwise.type)
    }

    override fun onActivate() {
        _type.set(result)
    }

    override fun doRecompute() {
        addConstraint(condition.type.now, BooleanT)
        addConstraint(then.type.now, result)
        addConstraint(otherwise.type.now, result)
    }

    override fun children(): Collection<TypeInference> = listOf(condition, then, otherwise)
}