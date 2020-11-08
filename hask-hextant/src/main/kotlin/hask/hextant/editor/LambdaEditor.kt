/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr
import hask.core.ast.Expr.Lambda
import hask.hextant.eval.EvaluationEnv
import hask.hextant.eval.EvaluationEnv.Resolution.Parameter
import hask.hextant.ti.LambdaTypeInference
import hask.hextant.ti.env.TIContext
import hextant.context.Context
import hextant.core.editor.CompoundEditor
import hextant.core.editor.composeResult
import reaktive.set.asSet
import validated.*
import validated.reaktive.ReactiveValidated

class LambdaEditor(context: Context) : CompoundEditor<Lambda>(context), ExprEditor<Lambda> {
    val parameters by child(IdentifierListEditor(context))
    val body by child(ExprExpander(Util.withTIContext(context) { it.copy(env = it.env.child()) }))

    init {
        parameters.ensureNotEmpty()
    }

    constructor(context: Context, name: String = "", t: ExprEditor<*>?) : this(context) {
        parameters.addLast(IdentifierEditor(context, name))
        if (t != null) body.setEditor(t)
    }

    override val result: ReactiveValidated<Lambda> = composeResult(parameters, body)
    override val freeVariables = body.freeVariables - parameters.results.asSet().map { it.orNull() }

    override val inference = LambdaTypeInference(
        context[TIContext],
        parameters.results,
        body.inference
    )

    override fun collectReferences(variable: String, acc: MutableCollection<ValueOfEditor>) {
        if (parameters.results.now.none { it == valid(variable) }) {
            body.collectReferences(variable, acc)
        }
    }

    override fun buildEnv(env: EvaluationEnv) {
        for ((index, p) in parameters.results.now.withIndex()) {
            p.ifValid { name ->
                val v = inference.typeVars[index]
                val type = inference.context.unificator.substituteNow(v)
                env.put(name, Parameter(type))
            }
        }
    }

    override fun lookup(name: String): ExprEditor<Expr>? =
        if (parameters.results.now.any { it == valid(name) }) null else super.lookup(name)

    override fun substitute(env: Map<String, ExprEditor<Expr>>): ExprEditor<*> {
        val bound = parameters.results.now.mapNotNull { it.orNull() }
        body.substitute(env - bound)
        return this
    }
}