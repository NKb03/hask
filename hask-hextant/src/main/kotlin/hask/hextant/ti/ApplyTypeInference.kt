/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti

import hask.core.type.Type
import hask.core.type.Type.Var
import hask.core.type.functionType
import hask.hextant.ti.env.TIContext
import hextant.*
import reaktive.list.ReactiveList
import reaktive.list.binding.values
import reaktive.value.now
import reaktive.value.reactiveVariable

class ApplyTypeInference(
    context: TIContext,
    private val function: TypeInference,
    private val arguments: ReactiveList<TypeInference>
) : AbstractTypeInference(context) {
    private val argumentTypes = arguments.map { it.type }.values()
    private val ret by typeVariable()

    init {
        dependsOn(function.type, argumentTypes)
    }

    override fun children(): Collection<TypeInference> = arguments.now + function

    override fun doRecompute() {
        val f = function.type.now.ifErr { return }
        val args = argumentTypes.now.map { t -> t.ifErr { Var(freshName()) } }
        addConstraint(f, functionType(args, Var(ret)))
    }

    override fun onActivate() {
        _type.set(ok(Var(ret)))
    }

    private var _type = reactiveVariable(childErr<Type>())
    override val type get() = _type
}