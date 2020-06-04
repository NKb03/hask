package hask.hextant.editor

import hask.hextant.ti.env.ADTDefinitionEnv
import hextant.Context
import hextant.completion.CompletionStrategy
import hextant.completion.ConfiguredCompleter

object SimpleTypeCompleter : ConfiguredCompleter<Context, String>(CompletionStrategy.simple) {
    override fun completionPool(context: Context): Collection<String> = context[ADTDefinitionEnv].availableTypes + "int"
}