/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.ADTDef
import hask.hextant.ti.env.ADTDefinitionEnv
import hextant.Context
import hextant.base.CompoundEditor
import hextant.core.editor.composeResult
import hextant.extend
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