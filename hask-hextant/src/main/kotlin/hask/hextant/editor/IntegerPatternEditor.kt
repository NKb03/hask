/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Pattern
import hextant.Context
import hextant.EditorView
import hextant.base.AbstractEditor
import reaktive.value.binding.map
import validated.*
import validated.reaktive.ReactiveValidated

class IntegerPatternEditor(context: Context, val value: IntLiteralEditor) :
    AbstractEditor<Pattern, EditorView>(context) {
    constructor(context: Context) : this(context, IntLiteralEditor(context))

    init {
        children(value)
    }

    override val result: ReactiveValidated<Pattern> =
        value.result.map { it.map { lit -> Pattern.Integer(lit.num) }.or(invalidComponent()) }
}