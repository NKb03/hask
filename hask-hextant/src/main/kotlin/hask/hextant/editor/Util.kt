/**
 * @author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr
import hask.hextant.context.HaskInternal
import hask.hextant.ti.TypeInference
import hask.hextant.ti.env.TIContext
import hextant.context.Context
import hextant.context.extend
import hextant.core.Editor
import reaktive.value.now
import validated.ifValid

object Util {
    fun withTIContext(context: Context, create: (TIContext) -> TIContext): Context = context.extend {
        set(HaskInternal, TIContext, create(get(HaskInternal, TIContext)))
    }

    fun buildEnv(editor: ExprEditor<*>): Map<String, Expr> {
        val env = mutableMapOf<String, Expr>()
        generateSequence(editor.parent) { it.parent }.forEach { e ->
            if (e is LetEditor) {
                for (b in e.bindings.results.now) {
                    b.ifValid { (n, v) -> env.putIfAbsent(n, v) }
                }
            }
        }
        return env
    }


    fun inferences(editor: Editor<*>): Set<TypeInference> = when (editor) {
        is ExprExpander -> editor.editor.now?.let { inferences(it) }.orEmpty() + editor.inference
        is ExprListEditor -> editor.editors.now.flatMapTo(mutableSetOf()) { inferences(it) }
        is BindingListEditor -> editor.editors.now.flatMapTo(mutableSetOf()) { inferences(it.value) }
        is ExprEditor -> editor.children.now.flatMapTo(mutableSetOf()) { inferences(it) } + editor.inference
        else                 -> emptySet()
    }

    fun getProgram(editor: Editor<*>): ProgramEditor = when (val p = editor.parent) {
        is ProgramEditor -> p
        is Editor<*> -> getProgram(p)
        else             -> throw NoSuchElementException("No top level program found.")
    }
}
