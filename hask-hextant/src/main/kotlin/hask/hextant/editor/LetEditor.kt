/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr
import hask.core.ast.Expr.Let
import hask.hextant.context.HaskInternal
import hask.hextant.eval.EvaluationEnv
import hask.hextant.eval.EvaluationEnv.Resolution
import hask.hextant.ti.LetTypeInference
import hask.hextant.ti.env.TIContext
import hextant.*
import hextant.base.CompoundEditor
import reaktive.value.now

class LetEditor(context: Context) : CompoundEditor<Let>(context), ExprEditor<Let> {
    val bindings by child(BindingListEditor(context))
    val body by child(ExprExpander(context))

    init {
        children(bindings, body)
    }

    override val result: EditorResult<Let> = result2(bindings, body) { bs, b ->
        ok(Let(bs, b))
    }

    override fun collectReferences(variable: String, acc: MutableCollection<ValueOfEditor>) {
        if (bindings.editors.now.none { it.name.result.now == ok(variable) }) {
            for (b in bindings.editors.now) {
                b.value.collectReferences(variable, acc)
                body.collectReferences(variable, acc)
            }
        }
    }

    override val inference = LetTypeInference(
        context[HaskInternal, TIContext],
        bindings.editors.map { it.name.result to it.value.inference },
        body.inference,
        context.createConstraintsHolder()
    )

    override fun buildEnv(env: EvaluationEnv) {
        for (b in bindings.editors.now) {
            b.name.result.now.ifOk { name -> env.put(name, Resolution.Resolved(b.value)) }
        }
    }

    override fun substitute(name: String, editor: ExprEditor<Expr>): LetEditor = TODO()

    override fun evaluateOneStep(): ExprEditor<Expr> = TODO()

    override fun lookup(name: String): ExprEditor<Expr>? =
        bindings.editors.now.find { it.name.result.now == ok(name) }?.value
}