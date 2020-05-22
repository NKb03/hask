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

class ReferenceTypeInference(context: TIContext, private val name: EditorResult<String>) :
    AbstractTypeInference(context) {
    private val _type = reactiveVariable(childErr<Type>())
    private val binding by lazy {
        name.flatMap {
            val n = it.ifErr { return@flatMap reactiveValue(ChildErr) }
            context.env.resolve(n).map { t -> t ?: err("Unresolved reference: '$n'") }
        }
    }

    override val type: ReactiveValue<CompileResult<Type>> get() = _type
    override fun onActivate() {
        addObserver(_type.bind(binding))
        addObserver(type.forEach(::releaseAndUseNames))
    }

    override fun onDeactivate() {
        binding.dispose()
    }

    private fun releaseAndUseNames(new: CompileResult<Type>) {
        releaseAllNames()
        new.ifOk { t -> useNames(t.fvs(env = context.env.freeTypeVars.now)) }
    }
}