/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hextant.Context
import hextant.core.editor.ListEditor

class IdentifierListEditor(context: Context) : ListEditor<String, IdentifierEditor>(context) {
    constructor(context: Context, identifiers: List<String>) : this(context) {
        for (ident in identifiers) {
            addAt(editors.now.size, IdentifierEditor(context, ident))
        }
    }

    override fun createEditor(): IdentifierEditor = IdentifierEditor(context)
}