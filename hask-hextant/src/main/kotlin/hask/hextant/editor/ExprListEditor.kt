/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr
import hextant.Context
import hextant.core.editor.ListEditor

class ExprListEditor(context: Context) : ListEditor<Expr, ExprExpander>(context) {
    override fun createEditor(): ExprExpander = ExprExpander(context)

    override fun editorAdded(editor: ExprExpander, index: Int) {
        val p = parent
        if (p is ExprEditor && p.inference.active) {
            editor.inference.activate()
        }
    }

    override fun editorRemoved(editor: ExprExpander, index: Int) {
        editor.inference.deactivate()
    }
}