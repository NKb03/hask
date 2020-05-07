/**
 * @author Nikolaus Knop
 */

package hask.hextant.editor

import hask.hextant.context.HaskInternal
import hask.hextant.ti.TypeInference
import hask.hextant.ti.env.TIContext
import hextant.Context
import hextant.Editor
import reaktive.value.now

fun Context.withChildTIContext(): Context {
    return Context.newInstance(this) {
        set(HaskInternal, TIContext, get(HaskInternal, TIContext).child())
    }
}

fun Editor<*>.inferences(): Set<TypeInference> = when (this) {
    is ExprExpander      -> (editor.now?.inferences() ?: emptySet<TypeInference>()) + inference
    is ExprListEditor -> editors.now.flatMapTo(mutableSetOf()) { it.inferences() }
    is BindingListEditor -> editors.now.flatMapTo(mutableSetOf()) { it.value.inferences() }
    is ExprEditor        -> children.now.flatMapTo(mutableSetOf<TypeInference>()) { it.inferences() } + inference
    else                 -> emptySet()
}