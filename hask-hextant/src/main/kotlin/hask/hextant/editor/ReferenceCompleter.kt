package hask.hextant.editor

import hask.core.type.TypeScheme
import hask.hextant.context.HaskInternal
import hask.hextant.editor.ReferenceCompleter.Reference
import hask.hextant.ti.env.TIContext
import hextant.completion.Completion.Builder
import hextant.completion.CompletionStrategy
import hextant.completion.ConfiguredCompleter
import hextant.core.Editor

object ReferenceCompleter : ConfiguredCompleter<Editor<*>, Reference>(CompletionStrategy.simple) {
    override fun completionPool(context: Editor<*>): Collection<Reference> =
        context.context[HaskInternal, TIContext].env.now.entries.map { (name, type) -> Reference(name, type) }

    override fun extractText(context: Editor<*>, item: Reference): String? = item.name

    override fun Builder<Reference>.configure(context: Editor<*>) {
        infoText = context.context[HaskInternal, TIContext].displayTypeScheme(completion.type)
    }

    data class Reference(val name: String, val type: TypeScheme)
}