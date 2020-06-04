package hask.hextant.editor

import hask.core.ast.ADT
import hask.hextant.ti.env.ADTDefinitions
import hextant.Context
import hextant.completion.Completion.Builder
import hextant.completion.CompletionStrategy
import hextant.completion.ConfiguredCompleter

object ParameterizedADTCompleter : ConfiguredCompleter<Context, ADT>(CompletionStrategy.simple) {
    override fun completionPool(context: Context): Collection<ADT> = context[ADTDefinitions].abstractDataTypes.now

    override fun extractText(context: Context, item: ADT): String? = item.name

    override fun Builder<ADT>.configure(context: Context) {
        infoText = completion.toString()
    }
}