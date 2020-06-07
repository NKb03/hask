/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Pattern.Destructuring
import hextant.Context
import hextant.base.CompoundEditor
import hextant.core.editor.composeResult
import reaktive.set.ReactiveSet
import reaktive.set.asSet
import validated.reaktive.ReactiveValidated

class DestructuringPatternEditor(context: Context, text: String) : CompoundEditor<Destructuring>(context),
                                                                   PatternEditor<Destructuring> {
    constructor(context: Context): this(context, "")

    val constructor by child(IdentifierEditor(context, text))
    val arguments by child(PatternListEditor(context))

    override val result: ReactiveValidated<Destructuring> = composeResult(constructor, arguments)

    override val boundVariables: ReactiveSet<String> = arguments.editors.asSet().flatMap { it.boundVariables }
}