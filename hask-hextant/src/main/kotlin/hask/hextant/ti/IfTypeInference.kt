package hask.hextant.ti

import hask.core.type.Type
import hask.core.type.Type.Var
import hask.hextant.ti.Builtins.BoolT
import hask.hextant.ti.env.TIContext
import hextant.*
import reaktive.value.now
import reaktive.value.reactiveVariable

class IfTypeInference(
    context: TIContext,
    private val condition: TypeInference,
    private val then: TypeInference,
    private val otherwise: TypeInference
) : AbstractTypeInference(context) {
    private val result by typeVariable()

    private var _type = reactiveVariable(childErr<Type>())
    override val type get() = _type

    init {
        dependsOn(condition.type, then.type, otherwise.type)
    }

    override fun onActivate() {
        _type.set(ok(Var(result)))
    }

    override fun doRecompute() {
        condition.type.now.ifOk { t -> addConstraint(t, BoolT) }
        then.type.now.ifOk { t -> addConstraint(t, Var(result)) }
        otherwise.type.now.ifOk { t -> addConstraint(t, Var(result)) }
    }

    override fun children(): Collection<TypeInference> = listOf(condition, then, otherwise)
}