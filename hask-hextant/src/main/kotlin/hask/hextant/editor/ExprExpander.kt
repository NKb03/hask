/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr
import hask.hextant.context.HaskInternal
import hask.hextant.eval.EvaluationEnv
import hask.hextant.ti.*
import hask.hextant.ti.env.TIContext
import hask.hextant.ti.env.TIEnvWrapper
import hextant.Context
import hextant.core.editor.ConfiguredExpander
import hextant.core.editor.ExpanderConfig
import reaktive.value.now

class ExprExpander(context: Context) :
    ConfiguredExpander<Expr, ExprEditor<Expr>>(config, context), ExprEditor<Expr> {
    constructor(context: Context, editor: ExprEditor<*>?) : this(context) {
        if (editor != null) setEditor(editor)
    }

    override val inference = ExpanderTypeInference(
        editor,
        context[HaskInternal, TIContext],
        context.createConstraintsHolder()
    )

    private val myEnv = context[HaskInternal, TIContext].env

    private val envSynchronizer = editor.observe { _, _, new ->
        if (new != null) {
            val wrapper = new.inference.context.env as TIEnvWrapper
            wrapper.setWrapped(myEnv)
        }
    }

    //@ProvideCommand(shortName = "apply")
    fun wrapInApply() {
        val editor = editor.now ?: return
        val apply = ApplyEditor(context.withEnvWrapper(), editor, null)
        setEditor(apply)
        views { group.getViewOf(apply.argument).focus() }
    }

    //@ProvideCommand(shortName = "let")
    fun wrapInLet() {
        val editor = editor.now ?: return
        val let = LetEditor(context.withEnvWrapper(), IdentifierEditor(context), null, editor)
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
            registerConstant("let") { LetEditor(it.withEnvWrapper()) }
            registerConstant("apply") { ApplyEditor(it.withEnvWrapper()) }
            registerConstant("dec") { IntLiteralEditor(it.withEnvWrapper()) }
            registerConstant("lambda") { LambdaEditor(it.withEnvWrapper()) }
            registerConstant("get") { ValueOfEditor(it.withEnvWrapper()) }
            registerConstant("if") { IfEditor(it.withEnvWrapper()) }
            registerInterceptor { text, context ->
                val asInt = text.toIntOrNull()
                when {
                    asInt != null                                   ->
                        IntLiteralEditor(context.withEnvWrapper(), text)
                    text.matches(IdentifierEditor.IDENTIFIER_REGEX) ->
                        ValueOfEditor(context.withEnvWrapper(), text)
                    else                                            -> null
                }
            }
        }
    }
}