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
import reaktive.value.now

class LambdaEditor private constructor(
    context: Context,
    val parameter: IdentifierEditor,
    val body: ExprExpander
) : CompoundEditor<Lambda>(context), ExprEditor<Lambda> {
    constructor(context: Context, parameter: IdentifierEditor, body: ExprEditor<*>?)
            : this(context, parameter, ExprExpander(context.withChildTIContext(), body))

    constructor(context: Context) : this(context, IdentifierEditor(context), null)

    init {
        children(parameter, body)
    }

    override val result: EditorResult<Lambda> = result2(parameter, body) { param, body ->
        ok(Lambda(param, body))
    }

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