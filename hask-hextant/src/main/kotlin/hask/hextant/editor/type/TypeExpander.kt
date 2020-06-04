/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor.type

import hask.core.parse.IDENTIFIER_REGEX
import hask.core.type.Type
import hask.hextant.ti.env.ADTDefinitions
import hextant.Context
import hextant.core.editor.ConfiguredExpander
import hextant.core.editor.ExpanderConfig
import reaktive.value.now
import validated.valid

class TypeExpander(
    context: Context,
    editor: TypeEditor? = null
) : TypeEditor, ConfiguredExpander<Type, TypeEditor>(config, context, editor) {
    override fun onExpansion(editor: TypeEditor) {
        if (editor is ParameterizedADTEditor) {
            val adts = context[ADTDefinitions].abstractDataTypes.now
            val adt = adts.find { editor.name.result.now == valid(it.name) } ?: return
            if (adt.typeParameters.isNotEmpty()) {
                editor.typeArguments.resize(adt.typeParameters.size)
                val e = editor.typeArguments.editors.now.last()
                views {
                    group.getViewOf(e).focus()
                }
            }
        }
    }

    companion object {
        val config = ExpanderConfig<TypeEditor>().apply {
            registerConstant("->") { FuncTypeEditor(it) }
            registerConstant("function") { FuncTypeEditor(it) }
            registerConstant("adt") { ParameterizedADTEditor(it) }
            registerConstant("int") { SimpleTypeEditor(it, "int") }
            registerInterceptor { text, context ->
                if (IDENTIFIER_REGEX.matches(text)) SimpleTypeEditor(context, text) else null
            }
            registerInterceptor { text, context ->
                if (IDENTIFIER_REGEX.matches(text) && text.first().isUpperCase()) ParameterizedADTEditor(context, text)
                else null
            }
        }
    }
}