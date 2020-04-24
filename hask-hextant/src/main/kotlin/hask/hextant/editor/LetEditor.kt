/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr
import hask.core.ast.Expr.Let
import hask.hextant.context.HaskInternal
import hask.hextant.eval.EvaluationEnv
import hask.hextant.eval.EvaluationEnv.Resolution
import hask.hextant.ti.*
import hask.hextant.ti.env.TIContext
import hextant.*
import hextant.base.CompoundEditor
import reaktive.value.now

class LetEditor private constructor(
    context: Context,
    val name: IdentifierEditor,
    val value: ExprExpander,
    val body: ExprExpander
) : CompoundEditor<Let>(context), ExprEditor<Let> {
    init {
        children(name, value, body)
    }

    constructor(context: Context, name: IdentifierEditor, value: ExprEditor<*>?, body: ExprEditor<*>?) : this(
        context,
        name,
        ExprExpander(context.withChildTIContext(), value),
        ExprExpander(context.withChildTIContext(), body)
    )

    constructor(context: Context) : this(context, IdentifierEditor(context), null, null)

    override val result: EditorResult<Let> = result3(name, value, body) { name, value, body ->
        ok(Let(name, value, body))
    }

    override fun collectReferences(variable: String, acc: MutableCollection<ValueOfEditor>) {
        if (name.result.now.orNull() != variable) {
            value.collectReferences(variable, acc)
            body.collectReferences(variable, acc)
        }
    }

    override val inference = LetTypeInference(
        context[HaskInternal, TIContext],
        name.result,
        value.inference,
        body.inference,
        context.createConstraintsHolder()
    )

    override fun buildEnv(env: EvaluationEnv) {
        name.result.now.ifOk { name -> env.put(name, Resolution.Resolved(value)) }
    }

    override fun substitute(name: String, editor: ExprEditor<Expr>): LetEditor = LetEditor(
        context,
        this.name,
        value.editor.now?.substitute(name, editor),
        body.editor.now?.substitute(name, editor)
    )

    override fun evaluateOneStep(): ExprEditor<Expr> {
        val v = value.editor.now ?: return this
        val b = body.editor.now ?: return this
        val n = name.result.now.ifErr { return this }
        return b.substitute(n, v)
    }

    override fun lookup(name: String): ExprEditor<Expr>? =
        if (name == this.name.result.now.orNull()) value.editor.now else null
}