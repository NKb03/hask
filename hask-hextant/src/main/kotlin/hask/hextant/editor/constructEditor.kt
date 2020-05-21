/**
 * @author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr
import hask.core.ast.Expr.*
import hextant.Context

fun Expr.constructEditor(context: Context): ExprEditor<Expr> = when (this) {
    is IntLiteral      -> IntLiteralEditor(context, num.toString())
    is ValueOf         -> ValueOfEditor(context, name)
    is Lambda          -> LambdaEditor(context).also { e ->
        e.parameters.setEditors(parameters.map { p -> IdentifierEditor(context, p) })
        e.body.setEditor(body)
    }
    is Apply           -> ApplyEditor(context).also { e ->
        e.applied.setEditor(function)
        e.arguments.setEditors(arguments.map {
            ExprExpander(e.arguments.context, it.constructEditor(e.arguments.context))
        })
    }
    is Let             -> LetEditor(context).also { e ->
        e.bindings.setEditors(bindings.map { b -> b.constructEditor(e.bindings.context) })
    }
    is If              -> IfEditor(context).also { e ->
        e.condition.setEditor(cond)

    }
    else -> throw UnsupportedOperationException("Constructing editor for $this is not supported")
}

private fun ExprExpander.setEditor(expr: Expr) {
    setEditor(expr.constructEditor(context))
}

private fun Binding.constructEditor(context: Context) = BindingEditor(context).also { e ->
    e.name.setText(name)
    e.value.setEditor(value.constructEditor(e.value.context))
}