/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr.IntLiteral
import hask.hextant.ti.IntLiteralTypeInference
import hask.hextant.ti.env.TIContext
import hextant.context.Context
import hextant.core.editor.TokenEditor
import hextant.core.view.TokenEditorView
import reaktive.set.emptyReactiveSet
import validated.Validated
import validated.valid

class IntLiteralEditor(context: Context, text: String) : TokenEditor<IntLiteral, TokenEditorView>(context, text),
                                                         ExprEditor<IntLiteral> {
    constructor(context: Context) : this(context, "")

    override fun compile(token: String): Validated<IntLiteral> = valid(IntLiteral(token))

    override fun collectReferences(variable: String, acc: MutableCollection<ValueOfEditor>) {}

    override val freeVariables = emptyReactiveSet<String>()

    override val inference = IntLiteralTypeInference(context[TIContext])
}