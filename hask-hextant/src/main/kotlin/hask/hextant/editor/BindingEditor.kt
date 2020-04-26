/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Expr.Binding
import hextant.*
import hextant.base.CompoundEditor

class BindingEditor(context: Context) : CompoundEditor<Binding>(context) {
    val name by child(IdentifierEditor(context))
    val value by child(ExprExpander(context))

    override val result = result2(name, value) { n, v -> ok(Binding(n, v)) }
}