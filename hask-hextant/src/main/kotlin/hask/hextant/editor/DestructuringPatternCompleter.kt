package hask.hextant.editor

import hask.core.ast.ADTConstructor
import hask.hextant.ti.env.ADTDefinitions
import hextant.completion.Completion.Builder
import hextant.completion.CompletionStrategy
import hextant.completion.ConfiguredCompleter
import hextant.core.Editor

object DestructuringPatternCompleter : ConfiguredCompleter<Editor<*>, ADTConstructor>(CompletionStrategy.simple) {
    override fun completionPool(context: Editor<*>): Collection<ADTConstructor> =
        context.context[ADTDefinitions].constructors()

    override fun extractText(context: Editor<*>, item: ADTConstructor): String? = item.name

    override fun Builder<ADTConstructor>.configure(context: Editor<*>) {
        infoText = completion.toString()
    }
}
