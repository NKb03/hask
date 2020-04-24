/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti

import hask.core.type.TypeScheme
import hask.core.type.Type.Var
import hask.hextant.ti.unify.ConstraintsHolder
import hask.hextant.ti.unify.bind
import hask.hextant.ti.env.SimpleTIEnv
import hask.hextant.ti.env.TIContext
import hextant.*
import reaktive.value.*
import reaktive.value.binding.flatMap
import reaktive.value.binding.map

class LetTypeInference(
    context: TIContext,
    private val boundName: EditorResult<String>,
    private val boundType: TypeInference,
    private val bodyType: TypeInference,
    holder: ConstraintsHolder
) : AbstractTypeInference(context, holder) {
    private val bodyEnv = bodyType.context.env as SimpleTIEnv

    private val valueEnv = boundType.context.env as SimpleTIEnv

    private val boundTypeVarName = context.namer.freshName()

    private val boundTypeVar = Var(boundTypeVarName)

    private val generalBoundType: ReactiveValue<CompileResult<TypeScheme>> = boundType.type.flatMap { r ->
        val t = r.ifErr { return@flatMap reactiveValue(ChildErr) }
        context.unificator.substitute(t).flatMap {
            boundType.context.env.generalize(it).map(::ok)
        }
    }

    private val nameObserver = boundName.observe { _, old, new ->
        old.ifOk { bodyEnv.unbind(it) }
        ifOk(new, generalBoundType.now) { n, t -> bodyEnv.bind(n, t) }
        new.ifOk {
            val t = TypeScheme(emptySet(), boundTypeVar)
            valueEnv.bind(it, t)
            bodyEnv.bind(it, t)
        }
        old.ifOk {
            valueEnv.unbind(it)
            bodyEnv.unbind(it)
        }
    }

    private val boundTypeObserver = generalBoundType.observe { _, _, new ->
        ifOk(boundName.now, new) { n, t -> bodyEnv.bind(n, t) }
    }

    private val constraintObs = holder.bind(reactiveValue(ok(boundTypeVar)), boundType.type, this)

    init {
        ifOk(boundName.now, generalBoundType.now) { n, t -> bodyEnv.bind(n, t) }
        boundName.now.ifOk { valueEnv.bind(it, TypeScheme(emptySet(), boundTypeVar)) }
    }

    override fun dispose() {
        super.dispose()
        nameObserver.kill()
        boundTypeObserver.kill()
        constraintObs.kill()
        boundName.now.ifOk { bodyEnv.unbind(it) }
        boundType.dispose()
        bodyType.dispose()
    }

    override val type = bodyType.type
}