/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.ADTDef
import hextant.Context
import hextant.base.CompoundEditor
import hextant.core.editor.composeResult
import validated.reaktive.ReactiveValidated

class ADTDefEditor(context: Context) : CompoundEditor<ADTDef>(context) {
    val adt by child(ADTEditor(context))
    val constructors by child(ADTConstructorListEditor(context))

    override val result: ReactiveValidated<ADTDef> = composeResult(adt, constructors)
}