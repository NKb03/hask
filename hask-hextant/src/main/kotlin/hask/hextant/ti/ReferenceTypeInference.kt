/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti

import hask.core.type.Type
import hask.hextant.ti.env.TIContext
import hask.hextant.ti.unify.ConstraintsHolder
import hextant.*
import reaktive.value.*
import reaktive.value.binding.flatMap
import reaktive.value.binding.map

class ReferenceTypeInference(
    context: TIContext,
    name: EditorResult<String>,
    holder: ConstraintsHolder
) : AbstractTypeInference(context, holder) {
    private val usedVariables = mutableSetOf<String>()

    override fun dispose() {
        super.dispose()
        release.kill()
        for (v in usedVariables) context.namer.release(v)
    }

    override val type: ReactiveValue<CompileResult<Type>> = name.flatMap {
        val n = it.ifErr { return@flatMap reactiveValue(ChildErr) }
        context.env.resolve(n).map { t -> t ?: err("Unresolved reference: '$n'") }
    }

    private val release = type.forEach { new ->
        for (v in usedVariables) context.namer.release(v)
        usedVariables.clear()
        new.ifOk { t -> usedVariables.addAll(t.fvs(env = context.env.freeTypeVars.now)) }
    }
}