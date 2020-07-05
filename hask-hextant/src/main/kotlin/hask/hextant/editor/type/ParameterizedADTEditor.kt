/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor.type

import hask.core.type.Type
import hask.core.type.Type.ParameterizedADT
import hask.hextant.editor.IdentifierEditor
import hextant.context.Context
import hextant.core.editor.CompoundEditor
import hextant.core.editor.composeResult
import validated.reaktive.ReactiveValidated

class ParameterizedADTEditor(context: Context, text: String = "") : CompoundEditor<Type>(context), TypeEditor {
    val name by child(IdentifierEditor(context, text))
    val typeArguments by child(TypeListEditor(context))

    override val result: ReactiveValidated<ParameterizedADT> = composeResult(name, typeArguments)
}