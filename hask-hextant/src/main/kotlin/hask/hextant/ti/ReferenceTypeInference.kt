/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti

import hask.core.type.Type
import hask.hextant.ti.unify.ConstraintsHolder
import hask.hextant.ti.env.TIContext
import hextant.*
import reaktive.value.*
import reaktive.value.binding.flatMap
import reaktive.value.binding.map

class ReferenceTypeInference(
    context: TIContext,
    name: EditorResult<String>,
    holder: ConstraintsHolder
) : AbstractTypeInference(context, holder) {
    override fun dispose() {
        super.dispose()
        release.kill()
        releaseVariables(type.now)
    }

    private fun releaseVariables(t: CompileResult<Type>) {
        t.ifOk {
            for (name in it.fvs()) {
                if (name !in context.env.freeTypeVars.now) context.namer.release(name)
            }
        }
    }

    override val type: ReactiveValue<CompileResult<Type>> = name.flatMap {
        val n = it.ifErr { return@flatMap reactiveValue(ChildErr) }
        context.env.resolve(n).map { t -> t ?: err("Unresolved reference: '$n'") }
    }

    private val release = type.observe { _, old, _ -> releaseVariables(old) }
}