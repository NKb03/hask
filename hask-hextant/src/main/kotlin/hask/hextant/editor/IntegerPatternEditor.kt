/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Pattern
import hextant.Context
import hextant.base.CompoundEditor
import reaktive.set.ReactiveSet
import reaktive.set.emptyReactiveSet
import reaktive.value.binding.map
import validated.*
import validated.reaktive.ReactiveValidated

class IntegerPatternEditor(context: Context, text: String = "") : CompoundEditor<Pattern.Integer>(context),
                                                                  PatternEditor<Pattern.Integer> {
    val value by child(IntLiteralEditor(context, text))

    override val result: ReactiveValidated<Pattern.Integer> =
        value.result.map { it.map { lit -> Pattern.Integer(lit.text) }.or(invalidComponent()) }

    override val boundVariables: ReactiveSet<String> = emptyReactiveSet()
}