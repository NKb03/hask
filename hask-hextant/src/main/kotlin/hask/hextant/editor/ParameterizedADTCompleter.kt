package hask.hextant.editor

import hask.core.ast.ADT
import hask.hextant.ti.env.ADTDefinitions
import hextant.completion.Completion.Builder
import hextant.completion.CompletionStrategy
import hextant.completion.ConfiguredCompleter
import hextant.core.Editor

object ParameterizedADTCompleter : ConfiguredCompleter<Editor<*>, ADT>(CompletionStrategy.simple) {
    override fun completionPool(context: Editor<*>): Collection<ADT> =
        context.context[ADTDefinitions].abstractDataTypes.now

    override fun extractText(context: Editor<*>, item: ADT): String? = item.name

    override fun Builder<ADT>.configure(context: Editor<*>) {
        infoText = completion.toString()
    }
}