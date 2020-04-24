/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor.type

import hask.hextant.editor.IdentifierEditor
import hask.core.type.Type
import hextant.*
import hextant.base.AbstractEditor

class SimpleTypeEditor(context: Context, name: IdentifierEditor) : AbstractEditor<Type, EditorView>(context), TypeEditor {
    val name = name.moveTo(context)

    override val result: EditorResult<Type> = this.name.result.mapResult { types[it] ?: Type.Var(it) }

    companion object {
        private val types = mapOf("int" to Type.INT)
    }
}