/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Pattern
import hextant.Context
import hextant.core.editor.ListEditor

class PatternListEditor(context: Context) : ListEditor<Pattern, PatternExpander>(context) {
    override fun createEditor(): PatternExpander? = PatternExpander(context)
}