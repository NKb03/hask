/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.type.TypeScheme
import hask.core.type.argumentsToSaturate
import hask.hextant.editor.FunctionApplicationCompleter.ApplicationCompletion
import hask.hextant.ti.env.TIContext
import hextant.completion.Completion.Builder
import hextant.completion.CompletionStrategy
import hextant.completion.ConfiguredCompleter
import hextant.core.Editor

object FunctionApplicationCompleter : ConfiguredCompleter<Editor<*>, ApplicationCompletion>(CompletionStrategy.simple) {
    override fun completionPool(context: Editor<*>): Collection<ApplicationCompletion> {
        val ctx = context.context[TIContext]
        return ctx.env.now.entries.flatMap { (name, type) ->
            val subst = ctx.unificator.root().substituteNow(type.body)
            val arguments = subst.argumentsToSaturate()
            (1..arguments).map { args -> ApplicationCompletion(name, type, args) }
        }
    }

    data class ApplicationCompletion(val name: String, val type: TypeScheme, val arguments: Int) {
        override fun toString(): String = name + " _".repeat(arguments)
    }

    override fun Builder<ApplicationCompletion>.configure(context: Editor<*>) {
        infoText = context.context[TIContext].displayTypeScheme(completion.type)
    }
}