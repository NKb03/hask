/**
 * @author Nikolaus Knop
 */

package hask.hextant

import hask.core.ast.Expr.Lambda
import hask.core.parse.IDENTIFIER_REGEX
import hask.core.type.Type.Var
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

val unresolvedVariableInspection = inspection<ValueOfEditor> {
    id = "unresolved-variable"
    description = "Reports unresolved variables"
    isSevere(true)
    checkingThat {
        val ctx = inspected.context[TIContext]
        inspected.result.flatMap { r ->
            r.fold(
                onValid = { ctx.env.isResolved(it.name) },
                onInvalid = { reactiveValue(true) }
            )
        }
    }
    message { "${inspected.result.now.force()} cannot be resolved" }
}

val typeConstraintInspection = inspection<ExprEditor<*>> {
    id = "constraint-conflict"
    description = "Reports unresolvable constraints"
    isSevere(true)
    message {
        val (a, b) = inspected.inference.errors.now.first()
        "Types $a and $b cannot be unified"
    }
    checkingThat { inspected.inference.errors.isEmpty() }
}

val betaConversion = inspection<ApplyEditor> {
    id = "beta-conversion"
    description = "Reports lambda terms that are immediately applied to an argument"
    isSevere(false)
    message { "Unnecessary lambda abstraction" }
    preventingThat {
        inspected.applied.result.map {
            it.map { a -> a is Lambda }.ifInvalid { false }
        } and inspected.arguments.result.map { it.isValid }
    }
    addFix("Replace all usages of abstracted variable by argument", eval) { inspected.expander!! }
}

val typeParameterUnresolvedInspection = inspection<SimpleTypeEditor> {
    id = "type-parameter-unresolved"
    description = "Reports unresolved type parameters"
    isSevere(true)
    checkingThat {
        val env = inspected.context[ADTDefinitionEnv]
        inspected.result.flatMap { v ->
            v.fold(
                onValid = { t -> if (t is Var) env.isResolved(t.name) else reactiveValue(true) },
                onInvalid = { reactiveValue(true) }
            )
        }
    }
    message { "${inspected.result.now.force()} cannot be resolved" }
}

val invalidIdentifierInspection = inspection<TokenEditor<*, *>> {
    id = "invalid-identifier"
    description = "Reports invalid identifiers"
    isSevere(true)
    checkingThat { inspected.text.map { it.matches(IDENTIFIER_REGEX) } }
    message { "Invalid identifier '${inspected.text.now}'" }
}

val invalidIntLiteralInspection = inspection<IntLiteralEditor> {
    id = "invalid-int-literal"
    description = "Reports invalid integer literals"
    isSevere(true)
    checkingThat { inspected.text.map { it.toIntOrNull() != null } }
    message { "Invalid integer literal '${inspected.text.now}'" }
}

val unresolvedADTInspection = inspection<ParameterizedADTEditor> {
    id = "unresolved-adt"
    description = "Reports unresolved ADTs"
    isSevere(true)
    location { inspected.name }
    checkingThat {
        val adts = inspected.context[ADTDefinitions]
        val name = inspected.name.result
        adts.isResolved(name)
    }
    message { "Unresolved ADT ${inspected.name.result.now.force()}" }
}