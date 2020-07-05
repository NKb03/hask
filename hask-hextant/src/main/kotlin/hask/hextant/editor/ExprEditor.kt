/**
 * @author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr
import hask.core.type.Type
import hask.hextant.eval.EvaluationEnv
import hask.hextant.ti.TypeInference
import hextant.core.Editor
import reaktive.set.ReactiveSet
import reaktive.value.ReactiveValue

interface ExprEditor<out E : Expr> : Editor<E> {
    val type: ReactiveValue<Type> get() = inference.type

    val inference: TypeInference

    val freeVariables: ReactiveSet<String>

    fun collectReferences(variable: String, acc: MutableCollection<ValueOfEditor>)

    fun buildEnv(env: EvaluationEnv) {}

    fun canEvalOneStep(): Boolean = false

    fun lookup(name: String): ExprEditor<Expr>? = when (val p = parent) {
        is ExprEditor<*>  -> p.lookup(name)
        is ExprListEditor -> (p.parent as? ExprEditor<Expr>)?.lookup(name)
        else              -> null
    }

    fun substitute(env: Map<String, ExprEditor<Expr>>): ExprEditor<*> = this
}