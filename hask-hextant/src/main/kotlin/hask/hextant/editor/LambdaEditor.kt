/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr
import hask.core.ast.Expr.Lambda
import hask.hextant.context.HaskInternal
import hask.hextant.eval.EvaluationEnv
import hask.hextant.eval.EvaluationEnv.Resolution.Parameter
import hask.hextant.ti.*
import hask.hextant.ti.env.TIContext
import hextant.*
import hextant.base.CompoundEditor
import reaktive.set.*

class LambdaEditor(context: Context) : CompoundEditor<Lambda>(context), ExprEditor<Lambda> {
    val parameters by child(IdentifierListEditor(context))
    val body by child(ExprExpander(context.withTIContext { it.copy(env = it.env.child()) }))

    init {
        parameters.ensureNotEmpty()
    }

    constructor(context: Context, name: String = "", t: ExprEditor<*>?) : this(context) {
        parameters.addLast(IdentifierEditor(context, name))
        if (t != null) body.setEditor(t)
    }

    override val result: EditorResult<Lambda> = result2(parameters, body) { params, body ->
        ok(Lambda(params, body))
    }

    override val freeVariables = body.freeVariables - parameters.results.asSet().map { it.orNull() }

    override val inference = LambdaTypeInference(
        context[HaskInternal, TIContext],
        parameters.results,
        body.inference
    )

    override fun collectReferences(variable: String, acc: MutableCollection<ValueOfEditor>) {
        if (parameters.results.now.none { it == ok(variable) }) {
            body.collectReferences(variable, acc)
        }
    }

    override fun buildEnv(env: EvaluationEnv) {
        for ((index, p) in parameters.results.now.withIndex()) {
            p.ifOk { name ->
                val v = inference.typeVars[index]
                val type = inference.context.unificator.substituteNow(v)
                env.put(name, Parameter(type))
            }
        }
    }

    override fun lookup(name: String): ExprEditor<Expr>? =
        if (parameters.results.now.any { it == ok(name) }) null else super.lookup(name)

    override fun substitute(env: Map<String, ExprEditor<Expr>>): ExprEditor<*> {
        val bound = parameters.results.now.mapNotNull { it.orNull() }
        body.substitute(env - bound)
        return this
    }
}