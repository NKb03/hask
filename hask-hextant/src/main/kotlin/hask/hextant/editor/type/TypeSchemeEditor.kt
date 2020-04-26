/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor.type

import hask.core.type.TypeScheme
import hask.hextant.editor.IdentifierListEditor
import hextant.*
import hextant.base.AbstractEditor

class TypeSchemeEditor(
    context: Context,
    val parameters: IdentifierListEditor = IdentifierListEditor(context),
    type: TypeEditor? = null
) : AbstractEditor<TypeScheme, EditorView>(context) {
    val body = TypeExpander(context, type)

    override val result: EditorResult<TypeScheme> =
        result2(parameters, body) { params, type -> ok(TypeScheme(params, type)) }
}