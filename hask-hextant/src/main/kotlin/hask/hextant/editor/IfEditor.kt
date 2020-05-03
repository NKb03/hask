/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr
import hask.core.ast.Expr.If
import hask.hextant.context.HaskInternal
import hask.hextant.ti.*
import hask.hextant.ti.env.TIContext
import hextant.*
import hextant.base.CompoundEditor
import reaktive.set.ReactiveSet
import reaktive.value.now

class IfEditor(
    context: Context
) : CompoundEditor<If>(context), ExprEditor<If> {
    val condition by child(ExprExpander(context.withChildTIContext()))
    val ifTrue by child(ExprExpander(context.withChildTIContext()))
    val ifFalse by child(ExprExpander(context.withChildTIContext()))

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

    override val result: EditorResult<If> = result3(condition, ifTrue, ifFalse) { c, t, f -> ok(If(c, t, f)) }

    override val freeVariables = condition.freeVariables + ifTrue.freeVariables + ifFalse.freeVariables

    override val inference = IfTypeInference(
        context[HaskInternal, TIContext],
        condition.inference,
        ifTrue.inference,
        ifFalse.inference,
        context.createConstraintsHolder()
    )

    override fun collectReferences(variable: String, acc: MutableCollection<ValueOfEditor>) {
        condition.collectReferences(variable, acc)
        ifTrue.collectReferences(variable, acc)
        ifFalse.collectReferences(variable, acc)
    }

    override fun evaluateOneStep(): ExprEditor<Expr> {
        val cond = condition.editor.now as? ValueOfEditor ?: return this
        val value = cond.result.now.ifErr { return this }
        return when (value.name) {
            "True"  -> ifTrue
            "False" -> ifFalse
            else    -> this
        }
    }
}