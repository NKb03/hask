/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.ADTConstructor
import hextant.context.Context
import hextant.core.editor.ListEditor

class ADTConstructorListEditor(context: Context) : ListEditor<ADTConstructor, ADTConstructorEditor>(context) {
    init {
        ensureNotEmpty()
    }

    override fun createEditor(): ADTConstructorEditor? = ADTConstructorEditor(context)
}