/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor.type

import hask.core.type.TypeScheme
import hask.hextant.editor.IdentifierListEditor
import hextant.Context
import hextant.EditorView
import hextant.base.AbstractEditor
import hextant.core.editor.composeResult
import validated.reaktive.ReactiveValidated

class TypeSchemeEditor(
    context: Context,
    val parameters: IdentifierListEditor = IdentifierListEditor(context),
    type: TypeEditor? = null
) : AbstractEditor<TypeScheme, EditorView>(context) {
    val body = TypeExpander(context, type)

    override val result: ReactiveValidated<TypeScheme> = composeResult(parameters, body)
}