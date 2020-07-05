package hask.hextant.editor

import hask.core.ast.ADTConstructor
import hask.hextant.ti.env.ADTDefinitions
import hextant.context.Context
import hextant.completion.Completion.Builder
import hextant.completion.CompletionStrategy
import hextant.completion.ConfiguredCompleter

object DestructuringPatternCompleter : ConfiguredCompleter<Context, ADTConstructor>(CompletionStrategy.simple) {
    override fun completionPool(context: Context): Collection<ADTConstructor> = context[ADTDefinitions].constructors()

    override fun extractText(context: Context, item: ADTConstructor): String? = item.name

    override fun Builder<ADTConstructor>.configure(context: Context) {
        infoText = completion.toString()
    }
}
