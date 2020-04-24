/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Pattern
import hextant.*
import hextant.base.AbstractEditor
import reaktive.value.binding.map

class IntegerPatternEditor(context: Context, val value: IntLiteralEditor) :
    AbstractEditor<Pattern, EditorView>(context) {
    constructor(context: Context) : this(context, IntLiteralEditor(context))

    init {
        children(value)
    }

    override val result: EditorResult<Pattern> =
        value.result.map { it.map { lit -> Pattern.Integer(lit.num) }.or(childErr()) }
}