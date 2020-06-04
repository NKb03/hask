package hask.hextant.editor

import hask.core.type.TypeScheme
import hask.hextant.context.HaskInternal
import hask.hextant.ti.env.TIContext
import hextant.Context
import hextant.completion.Completion.Builder
import hextant.completion.CompletionStrategy
import hextant.completion.ConfiguredCompleter

object ReferenceCompleter : ConfiguredCompleter<Context, Pair<String, TypeScheme>>(CompletionStrategy.simple) {
    override fun completionPool(context: Context): Collection<Pair<String, TypeScheme>> =
        context[HaskInternal, TIContext].env.now.entries.map { it.toPair() }

    override fun extractText(context: Context, item: Pair<String, TypeScheme>): String? = item.first

    override fun Builder<Pair<String, TypeScheme>>.configure(context: Context) {
        tooltipText
        infoText = context[HaskInternal, TIContext].displayTypeScheme(completion.second)
    }
}