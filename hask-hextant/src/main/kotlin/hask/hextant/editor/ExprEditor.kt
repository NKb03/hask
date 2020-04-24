/**
 * @author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr
import hask.core.type.Type
import hask.hextant.eval.EvaluationEnv
import hask.hextant.ti.*
import hextant.CompileResult
import hextant.Editor
import reaktive.value.ReactiveValue

interface ExprEditor<out E : Expr> : Editor<E> {
    val type: ReactiveValue<CompileResult<Type>> get() = inference.type

    val inference: TypeInference

    fun collectReferences(variable: String, acc: MutableCollection<ValueOfEditor>)

    //@ProvideCommand(shortName = "eval")
    fun evaluateOnce() {
        val exp = expander as? ExprExpander ?: return
        val new = evaluateOneStep()
        if (new !== this) {
            exp.setEditor(new)
        }
    }

    //@ProvideCommand(shortName = "eval!")
    fun evaluateFully() {
        TODO("not implemented")
    }

    //@ProvideCommand(shortName = "uneval")
    fun unevaluate() {
        TODO("not implemented")
    }

    //@ProvideCommand(shortName = "uneval!")
    fun unevaluateFully() {
        TODO("not implemented")
    }

    override fun supportsCopyPaste(): Boolean = true

    fun buildEnv(env: EvaluationEnv) {}

    fun evaluateOneStep(): ExprEditor<Expr> = this

    fun lookup(name: String): ExprEditor<Expr>? = (parent as? ExprEditor<*>)?.lookup(name)

    fun substitute(name: String, editor: ExprEditor<Expr>): ExprEditor<*> = this
}