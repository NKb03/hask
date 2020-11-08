/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr
import hask.core.ast.Expr.If
import hask.hextant.ti.IfTypeInference
import hask.hextant.ti.env.TIContext
import hextant.context.Context
import hextant.core.editor.CompoundEditor
import hextant.core.editor.composeResult
import reaktive.value.now
import validated.ifInvalid
import validated.reaktive.ReactiveValidated

class IfEditor(
    context: Context
) : CompoundEditor<If>(context), ExprEditor<If> {
    val condition by child(ExprExpander(context))
    val ifTrue by child(ExprExpander(context))
    val ifFalse by child(ExprExpander(context))

    constructor(
        context: Context,
        cond: ExprEditor<*>? = null,
        then: ExprEditor<*>? = null,
        otherwise: ExprEditor<*>? = null
    ) : this(context) {
        if (cond != null) condition.setEditor(cond)
        if (then != null) ifTrue.setEditor(then)
        if (otherwise != null) ifFalse.setEditor(otherwise)
    }

    override val result: ReactiveValidated<If> = composeResult(condition, ifTrue, ifFalse)

    override val freeVariables = condition.freeVariables + ifTrue.freeVariables + ifFalse.freeVariables

    override val inference = IfTypeInference(
        context[TIContext],
        condition.inference,
        ifTrue.inference,
        ifFalse.inference
    )

    override fun collectReferences(variable: String, acc: MutableCollection<ValueOfEditor>) {
        condition.collectReferences(variable, acc)
        ifTrue.collectReferences(variable, acc)
        ifFalse.collectReferences(variable, acc)
    }

    override fun substitute(env: Map<String, ExprEditor<Expr>>): ExprEditor<*> {
        condition.substitute(env)
        ifTrue.substitute(env)
        ifFalse.substitute(env)
        return this
    }

    override fun canEvalOneStep(): Boolean {
        val cond = condition.editor.now as? ValueOfEditor ?: return false
        val v = cond.result.now.ifInvalid { return false }
        return v.name in setOf("True", "False")
    }
}