/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr
import hextant.Context
import hextant.core.editor.ListEditor

class ExprListEditor(context: Context) : ListEditor<Expr, ExprExpander>(context) {
    override fun createEditor(): ExprExpander = ExprExpander(context)

    override fun editorRemoved(editor: ExprExpander, index: Int) {
        editor.inference.dispose()
    }
}