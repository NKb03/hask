package hask.hextant.editor

import hask.hextant.ti.env.ADTDefinitionEnv
import hextant.completion.CompletionStrategy
import hextant.completion.ConfiguredCompleter
import hextant.core.Editor

object SimpleTypeCompleter : ConfiguredCompleter<Editor<*>, String>(CompletionStrategy.simple) {
    override fun completionPool(context: Editor<*>): Collection<String> =
        context.context[ADTDefinitionEnv].availableTypes + "int"
}