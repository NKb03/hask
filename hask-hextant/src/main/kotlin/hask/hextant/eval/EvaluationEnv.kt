/**
 *@author Nikolaus Knop
 */

package hask.hextant.eval

import hask.core.ast.Expr
import hask.core.type.Type
import hask.hextant.editor.ExprEditor

class EvaluationEnv {
    private val definitions = mutableMapOf<String, Resolution>()
    fun resolve(name: String): Resolution = definitions[name] ?: Resolution.Unresolved

    fun put(name: String, definition: Resolution) {
        if (name !in definitions) {
            definitions[name] = definition
        }
    }

    sealed class Resolution {
        data class Resolved(val editor: ExprEditor<Expr>): Resolution()

        data class Parameter(val type: Type): Resolution()

        object Unresolved: Resolution()
    }

    companion object {
        fun forEditor(editor: ExprEditor<Expr>): EvaluationEnv {
            val env = EvaluationEnv()
            generateSequence(editor.parent) { editor.parent }.forEach {
                if (it is ExprEditor) it.buildEnv(env)
            }
            return env
        }
    }
}