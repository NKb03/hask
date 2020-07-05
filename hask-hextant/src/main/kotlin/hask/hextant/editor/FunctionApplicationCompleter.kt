/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.type.TypeScheme
import hask.core.type.argumentsToSaturate
import hask.hextant.context.HaskInternal
import hask.hextant.editor.FunctionApplicationCompleter.ApplicationCompletion
import hask.hextant.ti.env.TIContext
import hextant.context.Context
import hextant.completion.Completion.Builder
import hextant.completion.CompletionStrategy
import hextant.completion.ConfiguredCompleter

object FunctionApplicationCompleter : ConfiguredCompleter<Context, ApplicationCompletion>(CompletionStrategy.simple) {
    override fun completionPool(context: Context): Collection<ApplicationCompletion> {
        val ctx = context[HaskInternal, TIContext]
        return ctx.env.now.entries.flatMap { (name, type) ->
            val subst = ctx.unificator.root().substituteNow(type.body)
            val arguments = subst.argumentsToSaturate()
            (1..arguments).map { args -> ApplicationCompletion(name, type, args) }
        }
    }

    data class ApplicationCompletion(val name: String, val type: TypeScheme, val arguments: Int) {
        override fun toString(): String = name + " _".repeat(arguments)
    }

    override fun Builder<ApplicationCompletion>.configure(context: Context) {
        infoText = context[HaskInternal, TIContext].displayTypeScheme(completion.type)
    }
}