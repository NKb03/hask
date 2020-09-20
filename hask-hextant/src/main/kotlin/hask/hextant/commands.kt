/**
 * @author Nikolaus Knop
 */

package hask.hextant

import hask.hextant.editor.ExprExpander
import hask.hextant.editor.StringEditor
import hextant.command.Command.Type.SingleReceiver
import hextant.command.command
import hextant.context.createInput
import hextant.context.createOutput
import hextant.core.Editor
import reaktive.value.now
import java.nio.file.Paths

val eval = command<ExprExpander, Unit> {
    name = "eval"
    shortName = "eval"
    description = "Applies one step of evaluation"
    type = SingleReceiver
    applicableIf { it.editor.now?.canEvalOneStep() ?: false }
    executing { exp, _ -> exp.evaluateOnce() }
}

val save = command<Editor<*>, String> {
    name = "save"
    shortName = "save"
    description = "Saves the selected editor to the specified file"
    type = SingleReceiver
    addParameter<String> {
        editWith<StringEditor>()
        name = "file"
        description = "The destination file"
    }
    executing { editor, args ->
        val file = args[0] as String
        val path = Paths.get(file)
        val output = editor.context.createOutput(path)
        try {
            output.writeUntyped(editor)
            "Successfully saved to $path"
        } catch (ex: Throwable) {
            ex.message?.let { "Failure: $it" } ?: "Unknown error"
        }
    }
}
val open = command<Editor<*>, String> {
    name = "open"
    shortName = "open"
    description = "Reads the selected editor from the specified file"
    type = SingleReceiver
    addParameter<String> {
        editWith<StringEditor>()
        name = "file"
        description = "The input file"
    }
    executing { editor, args ->
        val file = args[0] as String
        val path = Paths.get(file)
        val input = editor.context.createInput(path)
        try {
            input.readInplace(editor)
            "Successfully read from $path"
        } catch (ex: Throwable) {
            ex.message?.let { "Failure: $it" } ?: "Unknown error"
        }
    }
}