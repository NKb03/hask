/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import bundles.set
import hask.core.ast.Program
import hask.hextant.ti.env.ADTDefinitions
import hask.hextant.ti.env.TIContext
import hextant.codegen.ProvideProjectType
import hextant.context.Context
import hextant.core.Editor
import hextant.core.editor.CompoundEditor
import hextant.core.editor.composeResult
import hextant.project.ProjectType
import validated.reaktive.ReactiveValidated

class ProgramEditor(context: Context) : CompoundEditor<Program>(context) {
    val adtDefs by child(ADTDefListEditor(context))
    val expr by child(ExprExpander(context))

    private val defs = ADTDefinitions(adtDefs.editors)
    private val observer = defs.bindConstructors(context[TIContext].env)

    init {
        context[ADTDefinitions] = defs
    }

    override val result: ReactiveValidated<Program> = composeResult(adtDefs, expr)

    @ProvideProjectType("Hask Project")
    companion object : ProjectType {
        override fun initializeContext(context: Context) {
            context[TIContext] = TIContext.root()
        }

        override fun createProject(context: Context): Editor<*> = ProgramEditor(context)
    }
}