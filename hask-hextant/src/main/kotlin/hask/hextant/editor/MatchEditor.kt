/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr.Match
import hask.hextant.ti.TypeInference
import hextant.Context
import hextant.base.CompoundEditor
import hextant.core.editor.composeResult
import reaktive.set.ReactiveSet
import validated.reaktive.ReactiveValidated

class MatchEditor(context: Context, val matched: ExprExpander) : CompoundEditor<Match>(context), ExprEditor<Match> {
    constructor(context: Context) : this(context, ExprExpander(context))

    val cases by child(CaseListEditor(context))

    override val result: ReactiveValidated<Match> = composeResult(matched, cases)
    override fun collectReferences(variable: String, acc: MutableCollection<ValueOfEditor>) {
        TODO("not implemented")
    }

    override val freeVariables: ReactiveSet<String>
        get() = TODO("not implemented")
    override val inference: TypeInference
        get() = TODO("not implemented")
}