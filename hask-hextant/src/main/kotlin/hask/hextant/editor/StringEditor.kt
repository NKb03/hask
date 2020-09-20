/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hextant.context.Context
import hextant.core.editor.TokenEditor
import hextant.core.view.TokenEditorView
import validated.Validated
import validated.valid

class StringEditor(context: Context) : TokenEditor<String, TokenEditorView>(context) {
    override fun compile(token: String): Validated<String> = valid(token)
}