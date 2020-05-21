/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti

import hask.core.type.Type
import hask.hextant.ti.env.TIContext
import hextant.*
import reaktive.value.*
import reaktive.value.binding.flatMap
import reaktive.value.binding.map

class ReferenceTypeInference(context: TIContext, name: EditorResult<String>) : NewAbstractTypeInference(context) {
    override val type: ReactiveValue<CompileResult<Type>> = name.flatMap {
        val n = it.ifErr { return@flatMap reactiveValue(ChildErr) }
        context.env.resolve(n).map { t -> t ?: err("Unresolved reference: '$n'") }
    }


    override fun onActivate() {
        val release = type.forEach { new ->
            releaseAllNames()
            new.ifOk { t -> useNames(t.fvs(env = context.env.freeTypeVars.now)) }
        }
        addObserver(release)
    }
}