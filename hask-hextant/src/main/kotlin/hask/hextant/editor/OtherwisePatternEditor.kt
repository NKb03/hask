/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Pattern
import hextant.*
import hextant.base.AbstractEditor
import reaktive.value.reactiveValue

class OtherwisePatternEditor(context: Context) : AbstractEditor<Pattern, EditorView>(context) {
    override val result: EditorResult<Pattern> = reactiveValue(ok(Pattern.Otherwise))
}