/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti

import hask.core.type.Type
import hask.hextant.ti.unify.ConstraintsHolder
import hask.hextant.ti.env.TIContext
import hextant.*
import reaktive.value.binding.flatMap
import reaktive.value.binding.map
import reaktive.value.now
import reaktive.value.reactiveValue

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
                context.namer.release(name)
            }
        }
    }

    override val type = name.flatMap {
        val n = it.ifErr { return@flatMap reactiveValue(ChildErr) }
        context.env.resolve(n).map { t -> t.okOrErr { "Unresolved reference: $n" } }
    }

    private val release = type.observe { _, old, _ -> releaseVariables(old) }
}