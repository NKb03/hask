/**
 * @author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr
import hask.core.ast.Expr.Apply
import hask.hextant.context.HaskInternal
import hask.hextant.ti.*
import hask.hextant.ti.env.TIContext
import hextant.*
import hextant.base.CompoundEditor
import reaktive.set.ReactiveSet
import reaktive.value.now

class ApplyEditor(context: Context) : CompoundEditor<Apply>(context), ExprEditor<Apply> {
    val applied by child(ExprExpander(context.withChildTIContext()))
    val argument by child(ExprExpander(context.withChildTIContext()))
    constructor(context: Context, left: ExprEditor<*>?, right: ExprEditor<*>?) : this(context) {
        if (left != null) applied.setEditor(left)
        if (right != null) argument.setEditor(right)
    }

    override val result: EditorResult<Apply> = result2(applied, argument) { l, r -> ok(Apply(l, r)) }

    override val freeVariables = applied.freeVariables + argument.freeVariables

    override val inference = ApplyTypeInference(
        context[HaskInternal, TIContext],
        applied.inference,
        argument.inference,
        context.createConstraintsHolder()
    )

    override val type = inference.type

    override fun collectReferences(variable: String, acc: MutableCollection<ValueOfEditor>) {
        applied.collectReferences(variable, acc)
        argument.collectReferences(variable, acc)
    }

    override fun substitute(name: String, editor: ExprEditor<Expr>) =
        ApplyEditor(context, applied.substitute(name, editor), argument.substitute(name, editor))

    override fun evaluateOneStep(): ExprEditor<Expr> {
        val applied = applied.editor.now as? LambdaEditor ?: return this
        val name = applied.parameter.result.now.ifErr { return this }
        val body = applied.body.editor.now ?: return this
        return body.substitute(name, argument)
    }
}