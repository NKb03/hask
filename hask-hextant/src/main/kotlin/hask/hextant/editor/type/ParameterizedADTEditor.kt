/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor.type

import hask.core.type.Type
import hask.core.type.Type.ParameterizedADT
import hask.hextant.editor.IdentifierEditor
import hextant.Context
import hextant.base.CompoundEditor
import hextant.core.editor.composeResult
import validated.reaktive.ReactiveValidated

class ParameterizedADTEditor(context: Context) : CompoundEditor<Type>(context), TypeEditor {
    val name by child(IdentifierEditor(context))
    val typeArguments by child(TypeListEditor(context))

    override val result: ReactiveValidated<ParameterizedADT> = composeResult(name, typeArguments)
}