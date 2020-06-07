/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr.Match
import hask.core.type.boundVariables
import hask.hextant.context.HaskInternal
import hask.hextant.ti.TypeInference
import hask.hextant.ti.env.TIContext
import hextant.Context
import hextant.base.CompoundEditor
import hextant.core.editor.composeResult
import reaktive.set.ReactiveSet
import reaktive.set.asSet
import reaktive.value.now
import validated.force
import validated.reaktive.ReactiveValidated

class MatchEditor(context: Context) : CompoundEditor<Match>(context), ExprEditor<Match> {
    val matched by child(ExprExpander(context))

    val cases by child(CaseListEditor(context))

    override val result: ReactiveValidated<Match> = composeResult(matched, cases)

    override fun collectReferences(variable: String, acc: MutableCollection<ValueOfEditor>) {
        for (case in cases.editors.now) {
            val p = case.pattern.result.now.force()
            if (variable !in p.boundVariables()) case.body.collectReferences(variable, acc)
        }
    }

    override val freeVariables: ReactiveSet<String> =
        cases.editors.asSet().flatMap { it.body.freeVariables - it.pattern.boundVariables }

    override val inference: TypeInference = MatchTypeInference(
        context[HaskInternal, TIContext],
        matched.inference,
        cases.editors.map { it.pattern.result to it.body.inference }
    )
}