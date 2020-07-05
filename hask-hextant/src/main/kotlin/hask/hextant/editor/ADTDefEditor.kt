/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.ADTDef
import hask.hextant.ti.env.ADTDefinitionEnv
import hextant.context.Context
import hextant.context.extend
import hextant.core.editor.CompoundEditor
import hextant.core.editor.composeResult
import reaktive.set.asSet
import reaktive.set.binding.mapNotNull
import validated.orNull
import validated.reaktive.ReactiveValidated

class ADTDefEditor(context: Context) : CompoundEditor<ADTDef>(context) {
    val adt by child(ADTEditor(context))
    val constructors by child(ADTConstructorListEditor(constructorsContext(context)))

    private fun constructorsContext(context: Context) = context.extend {
        val typeParameters = adt.parameters.results.asSet().mapNotNull { it.orNull() }
        set(ADTDefinitionEnv, ADTDefinitionEnv(typeParameters))
    }

    override val result: ReactiveValidated<ADTDef> = composeResult(adt, constructors)
}