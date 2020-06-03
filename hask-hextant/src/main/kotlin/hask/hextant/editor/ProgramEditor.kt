/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Program
import hextant.Context
import hextant.base.CompoundEditor
import hextant.core.editor.composeResult
import validated.reaktive.ReactiveValidated

class ProgramEditor(context: Context) : CompoundEditor<Program>(context) {
    val adtDefs by child(ADTDefListEditor(context))
    val expr by child(ExprExpander(context))

    override val result: ReactiveValidated<Program> = composeResult(adtDefs, expr)
}