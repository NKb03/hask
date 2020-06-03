/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.ADT
import hextant.Context
import hextant.base.CompoundEditor
import hextant.core.editor.composeResult
import validated.reaktive.ReactiveValidated

class ADTEditor(context: Context) : CompoundEditor<ADT>(context) {
    val name by child(IdentifierEditor(context))
    val parameters by child(IdentifierListEditor(context))

    override val result: ReactiveValidated<ADT> = composeResult(name, parameters)
}