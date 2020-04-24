/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor.type

import hask.hextant.editor.IdentifierEditor
import hask.core.type.Type
import hask.core.type.Type.*
import hextant.Context
import hextant.core.editor.Expander

class TypeExpander(context: Context, editor: TypeEditor? = null) : TypeEditor, Expander<Type, TypeEditor>(context) {
    init {
        if (editor != null) setEditor(editor)
    }

    override fun expand(text: String): TypeEditor? = when (text) {
        "->", "function" -> FuncTypeEditor(context)
        else -> parseType(text).orNull()?.let { editorForType(it) }
    }

    private fun editorForType(type: Type): TypeEditor? = when (type) {
        is Func             -> FuncTypeEditor(context, editorForType(type.from), editorForType(type.to))
        is ParameterizedADT -> null
        else -> SimpleTypeEditor(context, IdentifierEditor(context, type.toString()))
    }
}