/**
 * @author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Builtin
import hask.core.ast.Expr
import hask.core.ast.Expr.Apply
import hask.core.rt.isNormalForm
import hask.hextant.context.HaskInternal
import hask.hextant.ti.ApplyTypeInference
import hask.hextant.ti.env.TIContext
import hextant.context.Context
import hextant.context.withoutUndo
import hextant.core.editor.CompoundEditor
import hextant.core.editor.composeResult
import hextant.inspect.Inspections
import reaktive.set.asSet
import reaktive.value.now
import validated.force
import validated.isValid
import validated.reaktive.ReactiveValidated

class ApplyEditor(context: Context) : CompoundEditor<Apply>(context), ExprEditor<Apply> {
    val applied by child(ExprExpander(context))
    val arguments by child(ExprListEditor(context))

    constructor(context: Context, left: ExprEditor<*>) : this(context) {
        withoutUndo { applied.setEditor(left) }
    }

    override val result: ReactiveValidated<Apply> = composeResult(applied, arguments)

    override val freeVariables = applied.freeVariables + arguments.editors.asSet().flatMap { it.freeVariables }

    override val inference = ApplyTypeInference(
        context[HaskInternal, TIContext],
        applied.inference,
        arguments.editors.map { it.inference }
    )

    init {
        arguments.ensureNotEmpty()
    }

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

    override fun canEvalOneStep(): Boolean = when (val applied = applied.editor.now) {
        is LambdaEditor -> applied.parameters.result.now.isValid && applied.body.isExpanded
        is ValueOfEditor -> applied.text.now in Builtin.prelude
                && arguments.result.now.force().all { it.isNormalForm() }
                && context[Inspections].getProblems(this).isEmpty()
        else -> false
    }
}