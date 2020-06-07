/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr
import hask.core.parse.IDENTIFIER_REGEX
import hask.core.rt.*
import hask.hextant.context.HaskInternal
import hask.hextant.eval.EvaluationEnv
import hask.hextant.ti.ExpanderTypeInference
import hask.hextant.ti.env.TIContext
import hextant.Context
import hextant.base.EditorSnapshot
import hextant.command.Command.Type.SingleReceiver
import hextant.command.meta.ProvideCommand
import hextant.core.editor.ConfiguredExpander
import hextant.core.editor.ExpanderConfig
import hextant.snapshot
import hextant.undo.compoundEdit
import reaktive.set.binding.flattenToSet
import reaktive.set.emptyReactiveSet
import reaktive.value.binding.map
import reaktive.value.now
import validated.*
import java.util.*

class ExprExpander(context: Context, initial: ExprEditor<*>?) :
    ConfiguredExpander<Expr, ExprEditor<Expr>>(config, context, initial), ExprEditor<Expr> {

    constructor(context: Context) : this(context, null)

    override val freeVariables = editor.map { it?.freeVariables ?: emptyReactiveSet() }.flattenToSet()

    private val evalStack: Deque<EditorSnapshot<ExprEditor<Expr>>> = LinkedList()

    override fun onExpansion(editor: ExprEditor<Expr>) {
        if (this.inference.isActive) {
            //println("expanded, activating ${editor.result.now}")
            editor.inference.activate()
        }
    }

    override fun onReset(editor: ExprEditor<Expr>) {
        if (this.inference.isActive) {
            //println("reset, deactivating ${editor.result.now}")
            editor.inference.dispose()
        }
    }

    override fun defaultResult(): Validated<Expr> = valid(Expr.Hole)

    override val inference = ExpanderTypeInference(context[HaskInternal, TIContext], editor.map { it?.inference })

    @ProvideCommand(shortName = "apply", type = SingleReceiver)
    fun wrapInApply() {
        val editor = editor.now?.snapshot() ?: return
        val apply = ApplyEditor(context)
        context.compoundEdit("wrap in application") {
            setEditor(apply)
            apply.applied.paste(editor)
        }
        val arg1 = apply.arguments.editors.now.first()
        views {
            val view = group.getViewOf(arg1)
            view.focus()
        }
    }

    @ProvideCommand(shortName = "let", type = SingleReceiver)
    fun wrapInLet() {
        val editor = editor.now?.snapshot() ?: return
        val let = LetEditor(context)
        context.compoundEdit("wrap in let") {
            setEditor(let)
            val b = let.bindings.addLast()!!
            b.value.paste(editor)
        }
    }

    fun evaluateOnce() {
        val e = result.now.ifInvalid { return }
        saveSnapshot()
        val env = buildEnv()
        val r = e.evaluateOnce(env)
        if (r != null) reconstruct(r)
    }

    private fun saveSnapshot() {
        val old = editor.now ?: return
        evalStack.push(old.snapshot())
    }

    @ProvideCommand(shortName = "eval!", type = SingleReceiver)
    fun evaluateFully() {
        saveSnapshot()
        val e = result.now.ifInvalid { return }
        val f = StackFrame.root()
        val env = buildEnv()
        for ((n, v) in env) f.put(n, v.eval(f))
        val r = e.force(f)
        reconstruct(r.toExpr(env.keys))
    }

    fun unevaluate() {
        val e = evalStack.pop()
        paste(e)
    }

    fun unevaluateFully() {
        val e = evalStack.peekLast()
        evalStack.clear()
        paste(e)
    }

    override fun collectReferences(variable: String, acc: MutableCollection<ValueOfEditor>) {
        editor.now?.collectReferences(variable, acc)
    }

    override fun buildEnv(env: EvaluationEnv) {
        editor.now?.buildEnv(env)
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
            registerConstant("match") { MatchEditor(it) }
            registerInterceptor { text, context ->
                val asInt = text.toIntOrNull()
                when {
                    asInt != null                   ->
                        IntLiteralEditor(context, text)
                    text.matches(IDENTIFIER_REGEX) ->
                        ValueOfEditor(context, text)
                    else                            -> null
                }
            }
        }
    }
}