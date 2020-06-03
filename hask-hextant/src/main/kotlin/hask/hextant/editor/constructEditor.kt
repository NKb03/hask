/**
 * @author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr
import hask.core.ast.Expr.*


private fun <E : ExprEditor<*>> ExprExpander.setEditor(editor: E, initialize: (e: E) -> Unit) {
    setEditor(editor)
    initialize(editor)
}

fun ExprExpander.reconstruct(expr: Expr) {
    when (expr) {
        is IntLiteral -> setEditor(IntLiteralEditor(context, expr.num.toString()))
        is ValueOf    -> setEditor(ValueOfEditor(context, expr.name))
        is Lambda     -> setEditor(LambdaEditor(context)) { e ->
            e.parameters.setEditors(expr.parameters.map { p -> IdentifierEditor(context, p) })
            e.body.reconstruct(expr.body)
        }
        is Apply      -> setEditor(ApplyEditor(context)) { e ->
            e.applied.reconstruct(expr.function)
            e.arguments.resize(expr.arguments.size)
            for ((i, a) in expr.arguments.withIndex()) {
                e.arguments.editors.now[i].reconstruct(a)
            }
        }
        is Let        -> setEditor(LetEditor(context)) { e ->
            e.bindings.resize(expr.bindings.size)
            for ((i, b) in expr.bindings.withIndex()) {
                val be = e.bindings.editors.now[i]
                be.name.setText(b.name)
                be.value.reconstruct(b.value)
            }
            e.body.reconstruct(expr.body)
        }
        is If         -> setEditor(IfEditor(context)) { e ->
            e.condition.reconstruct(expr.cond)
            e.ifTrue.reconstruct(expr.then)
            e.ifFalse.reconstruct(expr.otherwise)
        }
        else          -> TODO()
    }
}