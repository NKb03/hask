/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Pattern
import hextant.Context
import hextant.EditorView
import hextant.base.AbstractEditor
import reaktive.value.reactiveValue
import validated.reaktive.ReactiveValidated
import validated.valid

class OtherwisePatternEditor(context: Context) : AbstractEditor<Pattern, EditorView>(context) {
    override val result: ReactiveValidated<Pattern> = reactiveValue(valid(Pattern.Otherwise))
}