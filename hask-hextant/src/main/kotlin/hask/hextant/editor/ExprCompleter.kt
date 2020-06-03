package hask.hextant.editor

import hask.hextant.context.HaskInternal
import hask.hextant.ti.env.TIContext
import hextant.Context
import hextant.completion.CompletionStrategy
import hextant.completion.ConfiguredCompleter

object ExprCompleter : ConfiguredCompleter<Context, String>(CompletionStrategy.simple) {
    private val keywords = "lambda, let, if, get, apply, decimal".split(", ").toSet()

    override fun completionPool(context: Context): Collection<String> = context[HaskInternal, TIContext].env.now.keys + keywords

    override fun extractText(context: Context, item: String): String? = item
}
