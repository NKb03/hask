/**
 * @author Nikolaus Knop
 */

package hask.hextant

import hask.core.ast.Expr.Lambda
import hask.core.parse.IDENTIFIER_REGEX
import hask.core.type.Type.Var
import hask.hextant.context.HaskInternal
import hask.hextant.editor.*
import hask.hextant.editor.type.ParameterizedADTEditor
import hask.hextant.editor.type.SimpleTypeEditor
import hask.hextant.ti.env.*
import hextant.core.editor.TokenEditor
import hextant.inspect.inspection
import reaktive.collection.binding.isEmpty
import reaktive.value.binding.*
import reaktive.value.now
import reaktive.value.reactiveValue
import validated.*

fun unresolvedVariableInspection(inspected: ValueOfEditor) = inspection(inspected) {
    description = "Reports unresolved variables"
    isSevere(true)
    val ctx = inspected.context[HaskInternal, TIContext]
    checkingThat(inspected.result.flatMap { r ->
        r.fold(
            onValid = { ctx.env.isResolved(it.name) },
            onInvalid = { reactiveValue(true) }
        )
    })
    message { "${inspected.result.now.force()} cannot be resolved" }
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
        it.map { a -> a is Lambda }.ifInvalid { false }
    } and inspected.arguments.result.map { it.isValid })
    addFix("Replace all usages of abstracted variable by argument", eval, inspected.expander!!)
}

fun typeParameterUnresolvedInspection(inspected: SimpleTypeEditor) = inspection(inspected) {
    description = "Reports unresolved type parameters"
    isSevere(true)
    val env = inspected.context[ADTDefinitionEnv]
    checkingThat(inspected.result.flatMap { v ->
        v.fold(
            onValid = { t -> if (t is Var) env.isResolved(t.name) else reactiveValue(true) },
            onInvalid = { reactiveValue(true) }
        )
    })
    message { "${inspected.result.now.force()} cannot be resolved" }
}

fun invalidIdentifierInspection(inspected: TokenEditor<*, *>) = inspection(inspected) {
    description = "Reports invalid identifiers"
    isSevere(true)
    checkingThat(inspected.text.map { it.matches(IDENTIFIER_REGEX) })
    message { "Invalid identifier '${inspected.text.now}'" }
}

fun invalidIntLiteralInspection(inspected: IntLiteralEditor) = inspection(inspected) {
    description = "Reports invalid integer literals"
    isSevere(true)
    checkingThat(inspected.text.map { it.toIntOrNull() != null })
    message { "Invalid integer literal '${inspected.text.now}'" }
}

fun unresolvedADTInspection(inspected: ParameterizedADTEditor) = inspection(inspected) {
    description = "Reports unresolved ADTs"
    isSevere(true)
    val adts = inspected.context[ADTDefinitions]
    val name = inspected.name.result
    location(inspected.name)
    checkingThat(adts.isResolved(name))
    message { "Unresolved ADT ${name.now.force()}" }
}