/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor.type

import hask.core.type.Type
import hextant.Context
import hextant.base.CompoundEditor
import hextant.core.editor.composeResult
import validated.reaktive.ReactiveValidated

class FuncTypeEditor(context: Context, from: TypeEditor? = null, to: TypeEditor? = null) :
    CompoundEditor<Type>(context), TypeEditor {
    val parameterType by child(TypeExpander(context, from))
    val resultType by child(TypeExpander(context, to))

    override val result: ReactiveValidated<Type.Func> = composeResult(parameterType, resultType)
}