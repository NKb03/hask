/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr
import hask.core.ast.Pattern
import hextant.context.Context
import hextant.core.editor.ListEditor

class CaseListEditor(context: Context) : ListEditor<Pair<Pattern, Expr>, CaseEditor>(context) {
    override fun createEditor(): CaseEditor = CaseEditor(context)

    override fun editorAdded(editor: CaseEditor, index: Int) {
        val p = parent
        if (p is ExprEditor && p.inference.isActive) {
            //println("activating ${editor.value.result.now}")
            editor.body.inference.activate()
        }
    }

    override fun editorRemoved(editor: CaseEditor, index: Int) {
        val p = parent
        if (p is ExprEditor && p.inference.isActive) {
            //println("deactivating ${editor.value.result.now}")
            editor.body.inference.dispose()
        }
    }
}