/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr
import hask.core.rt.StackFrame
import hask.core.rt.force
import hask.hextant.context.HaskInternal
import hask.hextant.eval.EvaluationEnv
import hask.hextant.ti.ExpanderTypeInference
import hask.hextant.ti.env.TIContext
import hextant.Context
import hextant.command.Command.Type.SingleReceiver
import hextant.command.meta.ProvideCommand
import hextant.core.editor.ConfiguredExpander
import hextant.core.editor.ExpanderConfig
import hextant.ifErr
import reaktive.set.binding.flattenToSet
import reaktive.set.emptyReactiveSet
import reaktive.value.binding.map
import reaktive.value.now
import java.util.*

class ExprExpander(context: Context) :
    ConfiguredExpander<Expr, ExprEditor<Expr>>(config, context), ExprEditor<Expr> {
    constructor(context: Context, editor: ExprEditor<*>?) : this(context) {
        if (editor != null) setEditor(editor)
    }

    override val freeVariables = editor.map { it?.freeVariables ?: emptyReactiveSet() }.flattenToSet()

    private val evalStack: Deque<ExprEditor<Expr>> = LinkedList()

    override fun onExpansion(editor: ExprEditor<Expr>) {
        if (this.inference.isActive) {
            println("expanded, activating ${editor.result.now}")
            editor.inference.activate()
        }
    }

    override fun onReset(editor: ExprEditor<Expr>) {
        println("reset, deactivating ${editor.result.now}")
        editor.inference.dispose()
    }

    override val inference = ExpanderTypeInference(context[HaskInternal, TIContext], editor.map { it?.inference })

    @ProvideCommand(shortName = "apply", type = SingleReceiver)
    fun wrapInApply() {
        val editor = editor.now ?: return
        val apply = ApplyEditor(context)
        apply.applied.setEditor(editor)
        setEditor(apply)
        views {
            val arg1 = apply.arguments.editors.now.first()
            val view = group.getViewOf(arg1)
            view.focus()
        }
    }

    @ProvideCommand(shortName = "let", type = SingleReceiver)
    fun wrapInLet() {
        val editor = editor.now ?: return
        val let = LetEditor(context)
        val b = let.bindings.addLast()!!
        b.value.setEditor(editor)
        setEditor(let)
    }

    fun evaluateOnce() {
        val e = editor.now ?: return
        evalStack.push(e)
        evaluateOneStep()
    }

    @ProvideCommand(shortName = "eval!", type = SingleReceiver)
    fun evaluateFully() {
        val e = result.now.ifErr { return }
        val f = StackFrame.root()
        val r = e.force(f)
        val old = editor.now!!
        evalStack.push(old)
        setEditor(r.toExpr().constructEditor(context))
    }

    fun unevaluate() {
        val e = evalStack.pop()
        setEditor(e)
    }

    fun unevaluateFully() {
        val e = evalStack.peekLast()
        evalStack.clear()
        setEditor(e)
    }

    override fun collectReferences(variable: String, acc: MutableCollection<ValueOfEditor>) {
        editor.now?.collectReferences(variable, acc)
    }

    override fun buildEnv(env: EvaluationEnv) {
        throw AssertionError()
    }

    override fun evaluateOneStep(): ExprEditor<Expr> {
        val e = editor.now ?: return this
        val new = e.evaluateOneStep()
        if (new is ExprExpander) {
            val c = new.editor.now
            if (c != null) setEditor(c)
            else reset()
        } else if (new !== e) setEditor(new)
        return this
    }

    override fun substitute(env: Map<String, ExprEditor<Expr>>): ExprExpander {
        val e = this.editor.now ?: return this
        val new = e.substitute(env)
        if (new is ExprExpander) {
            val c = new.editor.now
            if (c != null) setEditor(c)
            else reset()
        } else if (new !== e) setEditor(new)
        return this
    }

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