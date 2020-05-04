/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr.Binding
import hextant.Context
import hextant.core.editor.ListEditor

class BindingListEditor(context: Context) : ListEditor<Binding, BindingEditor>(context) {
    override fun createEditor(): BindingEditor? = BindingEditor(childContext())

    override fun childContext(): Context = context.withChildTIContext()

    override fun editorRemoved(editor: BindingEditor, index: Int) {
        editor.value.inference.dispose()
    }
}