/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr.IntLiteral
import hask.hextant.context.HaskInternal
import hask.hextant.ti.*
import hask.hextant.ti.env.TIContext
import hextant.*
import hextant.core.editor.TokenEditor
import hextant.core.view.TokenEditorView

class IntLiteralEditor(context: Context) : TokenEditor<IntLiteral, TokenEditorView>(context), ExprEditor<IntLiteral> {
    constructor(context: Context, text: String) : this(context) {
        setText(text)
    }

    override fun compile(token: String): CompileResult<IntLiteral> =
        token.toIntOrNull().okOrErr { "Invalid integer literal $token" }.map(::IntLiteral)

    override fun collectReferences(variable: String, acc: MutableCollection<ValueOfEditor>) {}

    override val inference = IntLiteralTypeInference(context[HaskInternal, TIContext], context.createConstraintsHolder())
}