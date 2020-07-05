/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Program
import hask.hextant.context.HaskInternal
import hask.hextant.ti.env.ADTDefinitions
import hask.hextant.ti.env.TIContext
import hextant.context.Context
import hextant.core.editor.CompoundEditor
import hextant.core.editor.composeResult
import validated.reaktive.ReactiveValidated

class ProgramEditor(context: Context) : CompoundEditor<Program>(context) {
    val adtDefs by child(ADTDefListEditor(context))
    val expr by child(ExprExpander(context))

    private val defs = ADTDefinitions(adtDefs.editors)
    private val observer = defs.bindConstructors(context[HaskInternal, TIContext].env)

    init {
        context[ADTDefinitions] = defs
    }

    override val result: ReactiveValidated<Program> = composeResult(adtDefs, expr)
}