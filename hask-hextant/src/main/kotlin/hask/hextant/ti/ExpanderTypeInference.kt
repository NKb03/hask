/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti

import hask.core.type.Type
import hask.hextant.editor.ExprEditor
import hask.hextant.ti.unify.ConstraintsHolder
import hask.hextant.ti.env.TIContext
import hextant.childErr
import hextant.or
import reaktive.value.*
import reaktive.value.binding.flatMap
import reaktive.value.binding.map

class ExpanderTypeInference(
    private val editor: ReactiveValue<ExprEditor<*>?>,
    context: TIContext,
    holder: ConstraintsHolder
) : AbstractTypeInference(context, holder) {
    override fun dispose() {
        super.dispose()
        obs.kill()
        editor.now?.inference?.dispose()
    }

    private val obs = editor.observe { old, _ ->
        if (!disposed) old?.inference?.dispose()
    }

    override val type = editor.flatMap {
        it?.inference?.type?.map { t -> t.or(childErr()) } ?: reactiveValue(childErr<Type>())
    }
}