package hask.hextant.editor

import hextant.Context
import hextant.completion.CompletionStrategy
import hextant.completion.ConfiguredCompleter

object KeywordExprCompleter : ConfiguredCompleter<Context, String>(CompletionStrategy.simple) {
    private val keywords = "lambda, let, if, get, apply, decimal".split(", ").toSet()

    override fun completionPool(context: Context): Collection<String> =  keywords

    override fun extractText(context: Context, item: String): String? = item
}
