/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Pattern
import hask.core.ast.Pattern.Integer
import hextant.context.Context
import hextant.core.editor.CompoundEditor
import reaktive.set.ReactiveSet
import reaktive.set.emptyReactiveSet
import reaktive.value.binding.map
import validated.*
import validated.reaktive.ReactiveValidated

class IntegerPatternEditor(context: Context, text: String = "") : CompoundEditor<Integer>(context),
                                                                  PatternEditor<Integer> {
    val value by child(IntLiteralEditor(context, text))

    override val result: ReactiveValidated<Integer> =
        value.result.map { it.map { lit -> Integer(lit.text) }.or(invalidComponent()) }

    override val boundVariables: ReactiveSet<String> = emptyReactiveSet()
}