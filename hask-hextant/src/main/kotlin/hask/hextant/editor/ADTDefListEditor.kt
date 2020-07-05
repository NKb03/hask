/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.ADTDef
import hextant.context.Context
import hextant.core.editor.ListEditor

class ADTDefListEditor(context: Context) : ListEditor<ADTDef, ADTDefEditor>(context) {
    override fun createEditor(): ADTDefEditor? = ADTDefEditor(context)
}