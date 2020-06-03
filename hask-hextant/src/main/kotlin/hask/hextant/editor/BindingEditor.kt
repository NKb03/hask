/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr.Binding
import hextant.Context
import hextant.base.CompoundEditor
import hextant.core.editor.composeResult
import validated.reaktive.ReactiveValidated
import validated.reaktive.composeReactive

class BindingEditor(context: Context) : CompoundEditor<Binding>(context) {
    val name by child(IdentifierEditor(context))
    val value by child(ExprExpander(context))

    override val result: ReactiveValidated<Binding> = composeResult(name, value)
}