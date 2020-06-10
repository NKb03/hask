/**
 * @author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr
import hask.core.ast.Pattern
import hask.hextant.ti.env.TIContext
import hextant.Context
import hextant.base.CompoundEditor
import hextant.core.editor.composeResult
import validated.reaktive.ReactiveValidated

class CaseEditor(context: Context) : CompoundEditor<Pair<Pattern, Expr>>(context) {
    val pattern by child(PatternExpander(context))

    val body by child(ExprExpander(context.withTIContext { it.child() }))

    override val result: ReactiveValidated<Pair<Pattern, Expr>> = composeResult(pattern, body)
}