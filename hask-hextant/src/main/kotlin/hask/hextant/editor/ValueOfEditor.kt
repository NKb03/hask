/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr
import hask.core.ast.Expr.ValueOf
import hask.hextant.context.HaskInternal
import hask.hextant.ti.ReferenceTypeInference
import hask.hextant.ti.env.TIContext
import hask.hextant.view.ValueOfEditorView
import hextant.*
import hextant.core.editor.TokenEditor
import reaktive.value.now

class ValueOfEditor(context: Context) : TokenEditor<ValueOf, ValueOfEditorView>(context), ExprEditor<ValueOf> {
    constructor(context: Context, text: String) : this(context) {
        setText(text)
    }

    override val inference = ReferenceTypeInference(
        context[HaskInternal, TIContext],
        result.mapResult { it.name },
        context.createConstraintsHolder()
    )

    override fun compile(token: String): CompileResult<ValueOf> = token
        .takeIf { it.matches(IdentifierEditor.IDENTIFIER_REGEX) }
        .okOrErr { "Invalid identifier $token" }
        .map { ValueOf(it) }

    override fun collectReferences(variable: String, acc: MutableCollection<ValueOfEditor>) {
        result.now.ifOk { if (it.name == variable) acc.add(this) }
    }

    fun setHighlighting(highlight: Boolean) {
        views { displayHighlighting(highlight) }
    }

    override fun evaluateOneStep(): ExprEditor<Expr> {
        val name = result.now.map { it.name }.ifErr { return this }
        return lookup(name)?.copyFor(this.context) ?: this
    }

    override fun substitute(name: String, editor: ExprEditor<Expr>): ExprEditor<*> =
        if (name == text.now) editor.copyFor(this.context) else this
}