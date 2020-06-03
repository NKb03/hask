/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor.type

import hask.core.type.Type
import hask.core.type.Type.Func
import hextant.Context
import hextant.EditorView
import hextant.base.AbstractEditor
import hextant.core.editor.composeResult
import validated.reaktive.ReactiveValidated
import validated.reaktive.composeReactive

class FuncTypeEditor(context: Context, from: TypeEditor? = null, to: TypeEditor? = null) :
    AbstractEditor<Type, EditorView>(context), TypeEditor {
    val parameterType = TypeExpander(context, from)
    val resultType = TypeExpander(context, to)

    override val result: ReactiveValidated<Type> = composeResult(parameterType, resultType)
}