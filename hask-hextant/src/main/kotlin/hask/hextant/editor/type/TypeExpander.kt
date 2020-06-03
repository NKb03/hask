/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor.type

import hask.core.type.*
import hask.core.type.Type.Func
import hask.core.type.Type.ParameterizedADT
import hextant.Context
import hextant.core.editor.Expander

class TypeExpander(context: Context, editor: TypeEditor? = null) :
    TypeEditor, Expander<Type, TypeEditor>(context, editor) {
    override fun expand(text: String): TypeEditor? = when (text) {
        "->", "function" -> FuncTypeEditor(context)
        "adt"            -> ParameterizedADTEditor(context)
        else             -> parseType(text).orNull()?.let { editorForType(it) }
    }

    private fun editorForType(type: Type): TypeEditor? = when (type) {
        is Func             -> FuncTypeEditor(context, editorForType(type.from), editorForType(type.to))
        is ParameterizedADT -> null
        else                -> SimpleTypeEditor(context, type.toString())
    }
}