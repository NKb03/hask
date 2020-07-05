/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Pattern.Variable
import hextant.context.Context
import hextant.core.editor.CompoundEditor
import hextant.core.editor.composeResult
import reaktive.set.ReactiveSet
import reaktive.set.toSet
import reaktive.value.binding.map
import validated.orNull
import validated.reaktive.ReactiveValidated

class VariablePatternEditor(context: Context, text: String) : CompoundEditor<Variable>(context),
                                                              PatternEditor<Variable> {
    constructor(context: Context): this(context, "")

    val identifier by child(IdentifierEditor(context, text))

    override val result: ReactiveValidated<Variable> = composeResult(identifier)

    override val boundVariables: ReactiveSet<String> = identifier.result.map { it.orNull() }.toSet()
}