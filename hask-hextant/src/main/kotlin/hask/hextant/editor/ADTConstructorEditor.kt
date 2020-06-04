/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.ADTConstructor
import hask.hextant.editor.type.TypeListEditor
import hextant.Context
import hextant.base.CompoundEditor
import hextant.core.editor.composeResult
import validated.reaktive.ReactiveValidated

class ADTConstructorEditor(context: Context) : CompoundEditor<ADTConstructor>(context) {
    val name by child(IdentifierEditor(context))
    val parameters by child(TypeListEditor(context))

    override val result: ReactiveValidated<ADTConstructor> = composeResult(name, parameters)
}