/**
 * @author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr
import hask.core.ast.Expr.Apply
import hask.hextant.context.HaskInternal
import hask.hextant.ti.ApplyTypeInference
import hask.hextant.ti.env.TIContext
import hextant.Context
import hextant.base.CompoundEditor
import hextant.core.editor.composeResult
import reaktive.set.asSet
import reaktive.value.now
import validated.isValid
import validated.reaktive.ReactiveValidated

class ApplyEditor private constructor(
    context: Context,
    left: ExprExpander,
    args: ExprListEditor
) : CompoundEditor<Apply>(context), ExprEditor<Apply> {
    val applied by child(left)
    val arguments by child(args)

    init {
        arguments.ensureNotEmpty()
    }

    constructor(context: Context) : this(context, ExprExpander(context), ExprListEditor(context))

    constructor(context: Context, left: ExprEditor<*>?, right: ExprEditor<*>?) : this(context) {
        if (left != null) applied.setEditor(left)
        if (right != null) arguments.editors.now[0].setEditor(right)
    }

    override val result: ReactiveValidated<Apply> = composeResult(applied, arguments)

    override val freeVariables = applied.freeVariables + arguments.editors.asSet().flatMap { it.freeVariables }

    override val inference = ApplyTypeInference(
        context[HaskInternal, TIContext],
        applied.inference,
        arguments.editors.map { it.inference }
    )

    override fun collectReferences(variable: String, acc: MutableCollection<ValueOfEditor>) {
        applied.collectReferences(variable, acc)
        for (e in arguments.editors.now) {
            e.collectReferences(variable, acc)
        }
    }

    override fun substitute(env: Map<String, ExprEditor<Expr>>): ApplyEditor {
        applied.substitute(env)
        for (a in arguments.editors.now) a.substitute(env)
        return this
    }

    override fun canEvalOneStep(): Boolean {
        val applied = applied.editor.now as? LambdaEditor ?: return false
        return applied.parameters.result.now.isValid && applied.body.isExpanded
    }
}