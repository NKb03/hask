/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr.IntLiteral
import hask.hextant.context.HaskInternal
import hask.hextant.ti.IntLiteralTypeInference
import hask.hextant.ti.env.TIContext
import hextant.*
import hextant.core.editor.TokenEditor
import hextant.core.view.TokenEditorView
import reaktive.set.emptyReactiveSet

class IntLiteralEditor(context: Context, text: String) : TokenEditor<IntLiteral, TokenEditorView>(context, text),
                                                         ExprEditor<IntLiteral> {
    constructor(context: Context) : this(context, "")

    override fun compile(token: String): CompileResult<IntLiteral> =
        token.toIntOrNull().okOrErr { "Invalid integer literal $token" }.map(::IntLiteral)

    override fun collectReferences(variable: String, acc: MutableCollection<ValueOfEditor>) {}

    override val freeVariables = emptyReactiveSet<String>()

    override val inference = IntLiteralTypeInference(context[HaskInternal, TIContext])
}