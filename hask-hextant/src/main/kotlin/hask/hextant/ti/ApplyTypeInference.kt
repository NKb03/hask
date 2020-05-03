/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti

import hask.core.type.Type
import hask.core.type.Type.Func
import hask.hextant.ti.env.TIContext
import hask.hextant.ti.unify.Constraint
import hask.hextant.ti.unify.ConstraintsHolder
import hextant.*
import reaktive.value.binding.binding
import reaktive.value.now

class ApplyTypeInference(
    context: TIContext,
    private val left: TypeInference,
    private val right: TypeInference,
    holder: ConstraintsHolder
) : AbstractTypeInference(context, holder) {
    private val returnTypeName = context.namer.freshName()

    private val returnType = Type.Var(returnTypeName)

    private val leftTypeObs = left.type.observe { _, _, new ->
        holder.clearConstraints()
        bind(new, right.type.now)
    }

    private val rightTypeObs = right.type.observe { _, _, new ->
        holder.clearConstraints()
        bind(left.type.now, new)
    }

    init {
        bind(left.type.now, right.type.now)
    }

    private fun bind(lt: CompileResult<Type>, rt: CompileResult<Type>) {
        if (lt is Ok && rt is Ok) {
            holder.addConstraint(
                Constraint(lt.value, Func(rt.value, returnType), this)
            )
        }
    }

    override fun dispose() {
        super.dispose()
        context.namer.release(returnTypeName)
        leftTypeObs.kill()
        rightTypeObs.kill()
        left.dispose()
        right.dispose()
    }

    override val type = binding(left.type, right.type) { lt, rt ->
        when {
            lt.isError -> childErr()
            rt.isError -> childErr()
            else       -> ok(returnType)
        }
    }
}