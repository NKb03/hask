/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor.type

import hask.core.type.Type
import hask.hextant.editor.IdentifierEditor
import hextant.context.Context
import hextant.core.editor.CompoundEditor
import validated.reaktive.ReactiveValidated
import validated.reaktive.composeReactive

class SimpleTypeEditor(context: Context, text: String = "") : CompoundEditor<Type>(context), TypeEditor {
    val name by child(IdentifierEditor(context, text))

    override val result: ReactiveValidated<Type> = composeReactive(name.result) { n -> types[n] ?: Type.Var(n) }

    companion object {
        private val types = mapOf("int" to Type.INT)
    }
}