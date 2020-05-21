/**
 * @author Nikolaus Knop
 */

package hask.hextant

import hask.core.ast.Expr.Lambda
import hask.hextant.editor.ApplyEditor
import hask.hextant.editor.ExprEditor
import hextant.*
import hextant.inspect.inspection
import reaktive.collection.binding.isEmpty
import reaktive.value.binding.and
import reaktive.value.binding.map
import reaktive.value.now

fun typeOkInspection(inspected: ExprEditor<*>) = inspection(inspected) {
    description = "Reports untypable terms"
    isSevere(true)
    message { (inspected.type.now as Err).message }
    preventingThat(inspected.type.map { it.isErr })
}

fun typeConstraintInspection(inspected: ExprEditor<*>) = inspection(inspected) {
    description = "Reports unresolvable constraints"
    isSevere(true)
    message {
        val (a, b) = inspected.inference.errors.now.first()
        "Types $a and $b cannot be unified"
    }
    checkingThat(inspected.inference.errors.isEmpty())
}

fun betaConversion(inspected: ApplyEditor) = inspection(inspected) {
    description = "Reports lambda terms that are immediately applied to an argument"
    isSevere(false)
    message { "Unnecessary lambda abstraction" }
    preventingThat(inspected.applied.result.map {
        it.map { a -> a is Lambda }.ifErr { false }
    } and inspected.arguments.result.map { it.isOk })
    addFix("Replace all usages of abstracted variable by argument", eval, inspected.expander!!)
}