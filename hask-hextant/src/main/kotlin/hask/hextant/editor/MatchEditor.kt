/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr.Match
import hask.hextant.ti.*
import hextant.*
import hextant.base.AbstractEditor
import hextant.base.CompoundEditor

class MatchEditor(context: Context, val matched: ExprExpander) : CompoundEditor<Match>(context), ExprEditor<Match> {
    constructor(context: Context): this(context, ExprExpander(context))

    val cases by child(CaseListEditor(context))

    override val result: EditorResult<Match> = result2(matched, cases) { expr, cases ->
        ok(Match(expr, cases.toMap()))
    }

    override fun collectReferences(variable: String, acc: MutableCollection<ValueOfEditor>) {
        TODO("not implemented")
    }

    override val inference: TypeInference
        get() = TODO("not implemented")
}