/**
 * @author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr
import hask.hextant.context.HaskInternal
import hask.hextant.ti.TypeInference
import hask.hextant.ti.env.TIContext
import hextant.*
import reaktive.value.now

fun Context.withTIContext(create: (TIContext) -> TIContext): Context {
    return Context.newInstance(this) {
        set(HaskInternal, TIContext, create(get(HaskInternal, TIContext)))
    }
}

fun Editor<*>.inferences(): Set<TypeInference> = when (this) {
    is ExprExpander      -> (editor.now?.inferences() ?: emptySet<TypeInference>()) + inference
    is ExprListEditor    -> editors.now.flatMapTo(mutableSetOf()) { it.inferences() }
    is BindingListEditor -> editors.now.flatMapTo(mutableSetOf()) { it.value.inferences() }
    is ExprEditor        -> children.now.flatMapTo(mutableSetOf<TypeInference>()) { it.inferences() } + inference
    else                 -> emptySet()
}

fun ExprEditor<*>.buildEnv(): Map<String, Expr> {
    val env = mutableMapOf<String, Expr>()
    generateSequence(parent) { it.parent }.forEach { e ->
        if (e is LetEditor) {
            for (b in e.bindings.results.now) {
                b.ifOk { (n, v) -> env.putIfAbsent(n, v) }
            }
        }
    }
    return env
}