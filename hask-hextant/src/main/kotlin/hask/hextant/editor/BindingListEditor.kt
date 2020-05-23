/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr.Binding
import hask.hextant.ti.unify.SimpleUnificator
import hextant.Context
import hextant.core.editor.ListEditor

class BindingListEditor(context: Context) : ListEditor<Binding, BindingEditor>(context) {
    override fun createEditor(): BindingEditor? = BindingEditor(childContext())

    override fun childContext(): Context =
        context.withTIContext { it.copy(unificator = it.unificator.child(), env = it.env.child()) }

    override fun editorAdded(editor: BindingEditor, index: Int) {
        val p = parent
        if (p is ExprEditor && p.inference.isActive) {
            //println("activating ${editor.value.result.now}")
            editor.value.inference.activate()
        }
    }

    override fun editorRemoved(editor: BindingEditor, index: Int) {
        val p = parent
        if (p is ExprEditor && p.inference.isActive) {
            //println("deactivating ${editor.value.result.now}")
            editor.value.inference.dispose()
        }
    }
}