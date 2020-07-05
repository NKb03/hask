/**
 * @author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Pattern
import hextant.core.Editor
import reaktive.set.ReactiveSet

interface PatternEditor<out P : Pattern> : Editor<P> {
    val boundVariables: ReactiveSet<String>
}