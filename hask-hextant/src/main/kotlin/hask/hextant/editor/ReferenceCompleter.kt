package hask.hextant.editor

import hask.hextant.context.HaskInternal
import hask.hextant.ti.env.TIContext
import hextant.Context
import hextant.completion.*
import hextant.completion.CompletionResult.Match

object ReferenceCompleter : Completer<Context, String> {
    override fun completions(context: Context, input: String): Collection<Completion<String>> {
        val ti = context[HaskInternal, TIContext]
        return ti.env.now.entries.mapNotNull { (n, t) ->
            val match = CompletionStrategy.simple.match(input, n) as? Match ?: return@mapNotNull null
            val desc = ti.displayTypeScheme(t)
            Completion(n, input, n, match.matchedRegions, "$n:$desc", desc, null)
        }.toSet()
    }
}
