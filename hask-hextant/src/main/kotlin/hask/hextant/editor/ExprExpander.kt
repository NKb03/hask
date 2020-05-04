/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr
import hask.hextant.context.HaskInternal
import hask.hextant.eval.EvaluationEnv
import hask.hextant.ti.*
import hask.hextant.ti.env.TIContext
import hextant.*
import hextant.command.Command.Type.SingleReceiver
import hextant.command.meta.ProvideCommand
import hextant.core.editor.ConfiguredExpander
import hextant.core.editor.ExpanderConfig
import reaktive.set.*
import reaktive.set.binding.flattenToSet
import reaktive.value.binding.map
import reaktive.value.now

class ExprExpander(context: Context) :
    ConfiguredExpander<Expr, ExprEditor<Expr>>(config, context), ExprEditor<Expr> {
    constructor(context: Context, editor: ExprEditor<*>?) : this(context) {
        if (editor != null) setEditor(editor)
    }

    override val freeVariables = editor.map { it?.freeVariables ?: emptyReactiveSet() }.flattenToSet()

    override val inference = ExpanderTypeInference(
        editor,
        context[HaskInternal, TIContext],
        context.createConstraintsHolder()
    )

    @ProvideCommand(shortName = "apply", type = SingleReceiver)
    fun wrapInApply() {
        val editor = editor.now ?: return
        val apply = ApplyEditor(context)
        apply.applied.setEditor(editor.copy())
        setEditor(apply)
        views { group.getViewOf(apply.argument).focus() }
    }

    @ProvideCommand(shortName = "let", type = SingleReceiver)
    fun wrapInLet() {
        val editor = editor.now ?: return
        val let = LetEditor(context)
        val b = let.bindings.addLast()!!
        b.value.setEditor(editor.copy())
        setEditor(let)
    }

    override fun collectReferences(variable: String, acc: MutableCollection<ValueOfEditor>) {
        editor.now?.collectReferences(variable, acc)
    }

    override fun buildEnv(env: EvaluationEnv) {
        throw AssertionError()
    }

    override fun evaluateOneStep(): ExprEditor<Expr> = ExprExpander(context, editor.now?.evaluateOneStep())

    override fun substitute(name: String, editor: ExprEditor<Expr>): ExprExpander =
        ExprExpander(context, this.editor.now?.substitute(name, editor))

    companion object {
        val config = ExpanderConfig<ExprEditor<Expr>>().apply {
            registerConstant("let") { LetEditor(it) }
            registerConstant("apply") { ApplyEditor(it) }
            registerConstant("dec") { IntLiteralEditor(it) }
            registerConstant("lambda") { LambdaEditor(it) }
            registerConstant("get") { ValueOfEditor(it) }
            registerConstant("if") { IfEditor(it) }
            registerInterceptor { text, context ->
                val asInt = text.toIntOrNull()
                when {
                    asInt != null                                   ->
                        IntLiteralEditor(context, text)
                    text.matches(IdentifierEditor.IDENTIFIER_REGEX) ->
                        ValueOfEditor(context, text)
                    else                                            -> null
                }
            }
        }
    }
}