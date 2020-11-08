/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr
import hask.core.ast.Expr.ValueOf
import hask.hextant.ti.ReferenceTypeInference
import hask.hextant.ti.env.TIContext
import hask.hextant.view.ValueOfEditorView
import hextant.context.Context
import hextant.core.editor.TokenEditor
import reaktive.set.toSet
import reaktive.value.binding.map
import reaktive.value.now
import validated.*

class ValueOfEditor(context: Context, text: String) : TokenEditor<ValueOf, ValueOfEditorView>(context, text),
                                                      ExprEditor<ValueOf> {
    constructor(context: Context) : this(context, "")

    override val freeVariables = result.map { it.orNull()?.name }.toSet()

    override val inference = ReferenceTypeInference(
        context[TIContext],
        result.map { r -> r.map { it.name } }
    )

    override fun compile(token: String): Validated<ValueOf> = valid(ValueOf(token))

    override fun collectReferences(variable: String, acc: MutableCollection<ValueOfEditor>) {
        result.now.ifValid { if (it.name == variable) acc.add(this) }
    }

    override fun canEvalOneStep(): Boolean =
        result.now.map { (name) -> name in Util.buildEnv(this) }.ifInvalid { false }

    fun setHighlighting(highlight: Boolean) {
        views { displayHighlighting(highlight) }
    }

    override fun substitute(env: Map<String, ExprEditor<Expr>>): ExprEditor<*> = env[text.now] ?: this
}