/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti

import hask.core.type.Type
import hask.core.type.Type.Func
import hask.core.type.Type.Var
import hask.hextant.ti.env.TIContext
import hask.hextant.ti.unify.ConstraintsHolder
import hextant.*
import reaktive.list.ListChange.*
import reaktive.list.ReactiveList
import reaktive.value.*

class LambdaTypeInference(
    context: TIContext,
    private val parameters: ReactiveList<CompileResult<String>>,
    private val bodyTypeInference: TypeInference,
    holder: ConstraintsHolder
) : AbstractTypeInference(context, holder) {
    private val bodyEnv get() = bodyTypeInference.context.env

    val typeVars = mutableListOf<String>()

    private val parametersObs = parameters.observeList { ch ->
        when (ch) {
            is Added    -> {
                val v = context.namer.freshName()
                typeVars.add(ch.index, v)
                bind(ch.element, Var(v))
            }
            is Removed  -> {
                val v = typeVars[ch.index]
                typeVars.removeAt(ch.index)
                context.namer.release(v)
                unbind(ch.element)
            }
            is Replaced -> {
                unbind(ch.old)
                bind(ch.new, Var(typeVars[ch.index]))
            }
        }
        recomputeType()
    }

    private fun bind(name: CompileResult<String>, t: Var) {
        name.ifOk { n ->
            if (parameters.now.count { it == ok(n) } == 1) bodyEnv.bind(n, t)
        }
    }

    private fun unbind(name: CompileResult<String>) {
        name.ifOk { n ->
            if (parameters.now.none { it == ok(n) }) bodyEnv.unbind(n)
        }
    }

    init {
        for (p in parameters.now) {
            val v = context.namer.freshName()
            typeVars.add(v)
            p.ifOk { name ->
                bodyEnv.bind(name, Var(v))
            }
        }
    }

    private var _type = reactiveVariable(childErr<Type>())

    private val bodyTypeObs = bodyTypeInference.type.forEach { recomputeType() }

    override fun dispose() {
        super.dispose()
        parametersObs.kill()
        bodyTypeObs.kill()
        for (n in typeVars) context.namer.release(n)
        bodyTypeInference.dispose()
    }

    private fun recomputeType() {
        _type.now = bodyTypeInference.type.now.map { t -> typeVars.foldRight(t) { v, acc -> Func(Var(v), acc) } }
    }

    override val type: ReactiveValue<CompileResult<Type>> = _type
}