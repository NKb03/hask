/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr
import hask.core.parse.IDENTIFIER_REGEX
import hask.core.rt.*
import hask.core.type.TopLevelEnv
import hask.hextant.eval.EvaluationEnv
import hask.hextant.ti.ExpanderTypeInference
import hask.hextant.ti.env.TIContext
import hextant.command.Command.Type.SingleReceiver
import hextant.command.meta.ProvideCommand
import hextant.context.Context
import hextant.context.EditorControlGroup
import hextant.core.editor.ConfiguredExpander
import hextant.core.editor.ExpanderConfig
import hextant.serial.Snapshot
import hextant.serial.snapshot
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

    private val evalStack: Deque<Snapshot<ExprEditor<Expr>>> = LinkedList()

    override fun onExpansion(editor: ExprEditor<Expr>) {
        if (editor is ApplyEditor) {
            val applied = editor.applied.result.now
            val args = editor.arguments.editors.now
            if (args.isNotEmpty() && applied.map { !it.containsHoles() }.ifInvalid { false }) {
                val e = args.first()
                views {
                    context[EditorControlGroup].getViewOf(e).focus()
                }
            }
        }
        if (this.inference.isActive) {
            editor.inference.activate()
        }
    }

    override fun onReset(editor: ExprEditor<Expr>) {
        if (this.inference.isActive) {
            editor.inference.dispose()
        }
    }

    override fun defaultResult(): Validated<Expr> = valid(Expr.Hole)

    override val inference = ExpanderTypeInference(context[TIContext], editor.map { it?.inference })

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
            val view = context[EditorControlGroup].getViewOf(arg1)
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
        val env = Util.buildEnv(this)
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
        val adts = Util.getProgram(this).adtDefs.results.now.mapNotNull { it.orNull() }
        TopLevelEnv(adts).putConstructorFunctions(f)
        val env = Util.buildEnv(this)
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
            "let" expand ::LetEditor
            "apply" expand ::ApplyEditor
            "dec" expand ::IntLiteralEditor
            "lambda" expand ::LambdaEditor
            "get" expand ::ValueOfEditor
            "if" expand ::IfEditor
            "match" expand ::MatchEditor
            registerInterceptor { text, context ->
                val asInt = text.toIntOrNull()
                when {
                    asInt != null                  -> IntLiteralEditor(context, text)
                    text.matches(IDENTIFIER_REGEX) -> ValueOfEditor(context, text)
                    text.endsWith("_")             -> {
                        val i = text.indexOf('_')
                        val name = text.take(i).trimEnd()
                        if (!name.matches(IDENTIFIER_REGEX)) null
                        else if (text.drop(i).any { it != ' ' && it != '_' }) null
                        else {
                            val args = text.drop(i).count { it == '_' }
                            val e = ApplyEditor(context, ValueOfEditor(context, name))
                            e.arguments.resize(args)
                            e
                        }
                    }
                    else                           -> null
                }
            }
        }
    }
}