/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr
import hask.core.ast.Expr.Let
import hask.hextant.context.HaskInternal
import hask.hextant.eval.EvaluationEnv
import hask.hextant.eval.EvaluationEnv.Resolution
import hask.hextant.ti.DependencyGraph
import hask.hextant.ti.LetTypeInference
import hask.hextant.ti.env.TIContext
import hextant.*
import hextant.base.CompoundEditor
import reaktive.set.asSet
import reaktive.value.now

class LetEditor(context: Context) : CompoundEditor<Let>(context), ExprEditor<Let> {
    val bindings by child(BindingListEditor(context))
    val body by child(ExprExpander(context.withTIContext { it.copy(env = it.env.child()) }))

    init {
        bindings.ensureNotEmpty()
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

    private val dependencyGraph = DependencyGraph(bindings.editors.map { b -> b.name.result to b.value.freeVariables })

    override val freeVariables =
        bindings.editors.asSet()
            .flatMap { it.value.freeVariables } + body.freeVariables - dependencyGraph.boundVariables

    override val inference = LetTypeInference(
        context[HaskInternal, TIContext],
        ::bindings,
        dependencyGraph,
        body.inference
    )

    private fun bindings() = bindings.editors.now.map { Pair(it.name.result.now, it.value.inference) }

    override fun buildEnv(env: EvaluationEnv) {
        for (b in bindings.editors.now) {
            b.name.result.now.ifOk { name -> env.put(name, Resolution.Resolved(b.value)) }
        }
    }

    override fun substitute(env: Map<String, ExprEditor<Expr>>): LetEditor {
        val bound = bindings.editors.now.mapNotNull { it.name.result.now.orNull() }
        val e = env - bound
        for (b in bindings.editors.now) b.value.substitute(e)
        body.substitute(e)
        return this
    }

    override fun canEvalOneStep(): Boolean = dependencyGraph.topologicalSort() != null

    override fun lookup(name: String): ExprEditor<Expr>? =
        bindings.editors.now.find { it.name.result.now == ok(name) }?.value ?: super.lookup(name)
}