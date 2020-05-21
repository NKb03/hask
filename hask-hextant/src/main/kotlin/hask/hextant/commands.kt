/**
 * @author Nikolaus Knop
 */

package hask.hextant

import hask.hextant.editor.ExprExpander
import hextant.command.Command.Type.SingleReceiver
import hextant.command.command
import reaktive.value.now

val eval = command<ExprExpander, Unit> {
    name = "eval"
    shortName = "eval"
    description = "Applies one step of evaluation"
    type = SingleReceiver
    applicableIf { it.editor.now?.canEvalOneStep() ?: false }
    executing { exp, _ -> exp.evaluateOnce() }
}
