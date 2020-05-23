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
import reaktive.set.toSet
import reaktive.value.binding.map
import reaktive.value.now

class ValueOfEditor(context: Context, text: String) : TokenEditor<ValueOf, ValueOfEditorView>(context, text),
                                                      ExprEditor<ValueOf> {
    constructor(context: Context): this(context, "")

    override val freeVariables = result.map { it.orNull()?.name }.toSet()

    override val inference = ReferenceTypeInference(context[HaskInternal, TIContext], result.mapResult { it.name })

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

    override fun substitute(env: Map<String, ExprEditor<Expr>>): ExprEditor<*> = env[text.now] ?: this
}