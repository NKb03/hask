/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor.type

import hask.core.type.Type
import hask.hextant.editor.IdentifierEditor
import hask.hextant.ti.env.ADTDefinitions
import hextant.Context
import hextant.core.editor.ConfiguredExpander
import hextant.core.editor.ExpanderConfig

class TypeExpander(
    context: Context,
    editor: TypeEditor? = null
) : TypeEditor, ConfiguredExpander<Type, TypeEditor>(config, context, editor) {
    companion object {
        val config = ExpanderConfig<TypeEditor>().apply {
            registerConstant("->") { FuncTypeEditor(it) }
            registerConstant("function") { FuncTypeEditor(it) }
            registerConstant("adt") { ParameterizedADTEditor(it) }
            registerConstant("int") { SimpleTypeEditor(it, "int") }
            registerInterceptor { text, context ->
                if (IdentifierEditor.IDENTIFIER_REGEX.matches(text)) SimpleTypeEditor(context, text) else null
            }
            registerInterceptor { text, context ->
                val adts = context[ADTDefinitions].abstractDataTypes.now
                val adt = adts.find { it.name == text } ?: return@registerInterceptor null
                ParameterizedADTEditor(context, adt.name).apply {
                    typeArguments.resize(adt.typeParameters.size)
                }
            }
        }
    }
}