/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr
import hask.core.ast.Expr.Lambda
import hask.hextant.context.HaskInternal
import hask.hextant.eval.EvaluationEnv
import hask.hextant.eval.EvaluationEnv.Resolution
import hask.hextant.ti.*
import hask.hextant.ti.env.TIContext
import hextant.*
import hextant.base.CompoundEditor
import reaktive.list.toList
import reaktive.set.*
import reaktive.value.binding.map
import reaktive.value.now

class LambdaEditor(context: Context) : CompoundEditor<Lambda>(context), ExprEditor<Lambda> {
    val parameter by child(IdentifierEditor(context))
    val body by child(ExprExpander(context.withChildTIContext()))

    constructor(context: Context, name: String = "", t: ExprEditor<*>?) : this(context) {
        parameter.setText(name)
        if (t != null) body.setEditor(t)
    }

    init {
        children(parameter, body)
    }

    override val result: EditorResult<Lambda> = result2(parameter, body) { param, body ->
        ok(Lambda(param, body))
    }

    override val freeVariables = body.freeVariables - parameter.result.map { it.orNull() }.toSet()

    override val inference = LambdaTypeInference(
        context[HaskInternal, TIContext],
        parameter.result,
        body.inference,
        context.createConstraintsHolder()
    )

    override fun collectReferences(variable: String, acc: MutableCollection<ValueOfEditor>) {
        if (parameter.result.now.map { n -> n != variable }.ifErr { true }) {
            body.collectReferences(variable, acc)
        }
    }

    override fun buildEnv(env: EvaluationEnv) {
        parameter.result.now.ifOk { name ->
            val type = inference.context.unificator.substituteNow(inference.parameterType)
            env.put(name, Resolution.Parameter(type))
        }
    }

    override fun lookup(name: String): ExprEditor<Expr>? =
        if (parameter.result.now.orNull() == name) null else super.lookup(name)
}