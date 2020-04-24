/**
 * @author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr
import hask.core.ast.Pattern
import hextant.*
import hextant.base.CompoundEditor

class CaseEditor(context: Context) : CompoundEditor<Pair<Pattern, Expr>>(context) {
    val pattern by child(PatternExpander(context))

    val body by child(ExprExpander(context))

    override val result: EditorResult<Pair<Pattern, Expr>> = result2(pattern, body) { pat, b ->
        ok(pat to b)
    }
}