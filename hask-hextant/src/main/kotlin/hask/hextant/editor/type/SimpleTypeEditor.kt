/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor.type

import hask.core.type.Type
import hask.hextant.editor.IdentifierEditor
import hextant.*
import hextant.base.AbstractEditor
import validated.reaktive.ReactiveValidated
import validated.reaktive.mapValidated

class SimpleTypeEditor(context: Context, name: IdentifierEditor) : AbstractEditor<Type, EditorView>(context), TypeEditor {
    val name = name.moveTo(context)

    override val result: ReactiveValidated<Type> = this.name.result.mapValidated { types[it] ?: Type.Var(it) }

    companion object {
        private val types = mapOf("int" to Type.INT)
    }
}