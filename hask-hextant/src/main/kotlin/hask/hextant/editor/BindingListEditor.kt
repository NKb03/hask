/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr.Binding
import hextant.Context
import hextant.core.editor.ListEditor
import reaktive.value.now

class BindingListEditor(context: Context) : ListEditor<Binding, BindingEditor>(context) {
    override fun createEditor(): BindingEditor? = BindingEditor(childContext())

    override fun childContext(): Context = context.withChildTIContext()

    override fun editorAdded(editor: BindingEditor, index: Int) {
        val p = parent
        if (p is ExprEditor && p.inference.active) {
            println("activating ${editor.value.result.now}")
            editor.value.inference.activate()
        }
    }

    override fun editorRemoved(editor: BindingEditor, index: Int) {
        println("deactivating ${editor.value.result.now}")
        editor.value.inference.deactivate()
    }
}