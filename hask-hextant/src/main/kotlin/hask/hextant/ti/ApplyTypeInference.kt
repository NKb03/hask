/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti

import hask.core.type.Type.Var
import hask.core.type.functionType
import hask.hextant.ti.env.TIContext
import hask.hextant.ti.unify.Constraint
import hask.hextant.ti.unify.ConstraintsHolder
import hextant.ifErr
import hextant.ok
import reaktive.dependencies
import reaktive.list.ReactiveList
import reaktive.list.binding.values
import reaktive.value.now
import reaktive.value.reactiveValue

class ApplyTypeInference(
    context: TIContext,
    private val function: TypeInference,
    private val arguments: ReactiveList<TypeInference>,
    holder: ConstraintsHolder
) : AbstractTypeInference(context, holder) {
    private val argumentTypes = arguments.map { it.type }.values()
    private val returnTypeName = context.namer.freshName()
    private val returnType = Var(returnTypeName)
    private val usedVariables = mutableSetOf<String>()
    private val obs = dependencies(function.type, argumentTypes).observe { updateConstraint() }

    init {
        updateConstraint()
    }

    private fun updateConstraint() {
        for (v in usedVariables) context.namer.release(v)
        usedVariables.clear()
        holder.clearConstraints()
        val f = function.type.now.ifErr { return }
        val args = argumentTypes.now.map { t ->
            val v = context.namer.freshName()
            usedVariables.add(v)
            t.ifErr { Var(v) }
        }
        val ft = functionType(args, returnType)
        holder.addConstraint(Constraint(f, ft, this))
    }

    override fun dispose() {
        super.dispose()
        context.namer.release(returnTypeName)
        for (v in usedVariables) context.namer.release(v)
        obs.kill()
        function.dispose()
        for (a in arguments.now) a.dispose()
    }

    override val type = reactiveValue(ok(returnType))
}