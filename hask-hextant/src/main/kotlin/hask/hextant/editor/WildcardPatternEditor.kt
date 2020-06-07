/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Pattern.Wildcard
import hextant.Context
import hextant.EditorView
import hextant.base.AbstractEditor
import hextant.base.CompoundEditor
import reaktive.set.ReactiveSet
import reaktive.set.emptyReactiveSet
import reaktive.value.reactiveValue
import validated.reaktive.ReactiveValidated
import validated.valid

class WildcardPatternEditor(context: Context) : CompoundEditor<Wildcard>(context), PatternEditor<Wildcard> {
    override val result: ReactiveValidated<Wildcard> = reactiveValue(valid(Wildcard))

    override val boundVariables: ReactiveSet<String> = emptyReactiveSet()
}