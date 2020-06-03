/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor


import hask.core.ast.Expr
import hask.core.ast.Pattern
import hextant.Context
import hextant.core.editor.ListEditor

class CaseListEditor(context: Context) : ListEditor<Pair<Pattern, Expr>, CaseEditor>(context) {
    override fun createEditor(): CaseEditor = CaseEditor(context)
}