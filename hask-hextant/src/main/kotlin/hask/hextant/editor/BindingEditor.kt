/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr.Binding
import hextant.context.Context
import hextant.core.editor.CompoundEditor
import hextant.core.editor.composeResult
import validated.reaktive.ReactiveValidated

class BindingEditor(context: Context) : CompoundEditor<Binding>(context) {
    val name by child(IdentifierEditor(context))
    val value by child(ExprExpander(context))

    override val result: ReactiveValidated<Binding> = composeResult(name, value)
}