/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor.type

import hask.core.type.Type
import hask.core.type.Type.Func
import hextant.*
import hextant.base.AbstractEditor

class FuncTypeEditor(context: Context, from: TypeEditor? = null, to: TypeEditor? = null)
    : AbstractEditor<Type, EditorView>(context), TypeEditor {
    val parameterType = TypeExpander(context, from)
    val resultType = TypeExpander(context, to)

    override val result: EditorResult<Type> = result2(parameterType, resultType) { p, t -> ok(Func(p, t)) }
}