package hask.hextant.editor

import hask.core.type.TypeScheme
import hask.hextant.context.HaskInternal
import hask.hextant.editor.ReferenceCompleter.Reference
import hask.hextant.ti.env.TIContext
import hextant.context.Context
import hextant.completion.Completion.Builder
import hextant.completion.CompletionStrategy
import hextant.completion.ConfiguredCompleter

object ReferenceCompleter : ConfiguredCompleter<Context, Reference>(CompletionStrategy.simple) {
    override fun completionPool(context: Context): Collection<Reference> =
        context[HaskInternal, TIContext].env.now.entries.map { (name, type) -> Reference(name, type) }

    override fun extractText(context: Context, item: Reference): String? = item.name

    override fun Builder<Reference>.configure(context: Context) {
        infoText = context[HaskInternal, TIContext].displayTypeScheme(completion.type)
    }

    data class Reference(val name: String, val type: TypeScheme)
}