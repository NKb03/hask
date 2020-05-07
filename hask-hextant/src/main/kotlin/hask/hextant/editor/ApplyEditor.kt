/**
 * @author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr
import hask.core.ast.Expr.Apply
import hask.hextant.context.HaskInternal
import hask.hextant.ti.ApplyTypeInference
import hask.hextant.ti.env.TIContext
import hextant.*
import hextant.base.CompoundEditor
import reaktive.set.asSet
import reaktive.value.now

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

    override val result: EditorResult<Apply> = result2(applied, arguments) { l, r -> ok(Apply(l, r)) }

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

    override fun evaluateOneStep(): ExprEditor<Expr> {
        val applied = applied.editor.now as? LambdaEditor ?: return this
        val parameters = applied.parameters.result.now.ifErr { return this }
        val body = applied.body.editor.now ?: return this
        val args = arguments.editors.now
        val env = parameters.zip(args).toMap()
        val b = body.substitute(env).copy()
        return when {
            args.size < parameters.size -> {
                val e = LambdaEditor(context)
                val params = parameters.drop(args.size).map { name -> IdentifierEditor(e.context, name) }
                e.parameters.setEditors(params)
                e.body.paste(b)
                e
            }
            args.size > parameters.size -> {
                val e = ApplyEditor(context)
                e.applied.paste(b)
                e.arguments.setEditors(args.drop(parameters.size).map { it.copy() })
                e
            }
            else                        -> b
        }
    }

    override fun canEvalOneStep(): Boolean {
        val applied = applied.editor.now as? LambdaEditor ?: return false
        return applied.parameters.result.now.isOk && applied.body.isExpanded
    }
}