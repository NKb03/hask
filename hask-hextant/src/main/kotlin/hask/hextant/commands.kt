/**
 * @author Nikolaus Knop
 */

package hask.hextant

import hask.hextant.editor.ExprExpander
import hask.hextant.editor.StringEditor
import hextant.command.Command.Type.SingleReceiver
import hextant.command.command
import hextant.core.Editor
import hextant.serial.Snapshot
import hextant.serial.saveSnapshotAsJson
import kotlinx.serialization.json.Json
import reaktive.value.now
import java.io.File

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
        val file = File(args[0] as String)
        try {
            editor.saveSnapshotAsJson(file)
            "Successfully saved to $file"
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
        val file = File(args[0] as String)
        try {
            val json = Json.parseToJsonElement(file.readText())
            val snapshot = Snapshot.decode<Editor<*>>(json)
            snapshot.reconstruct(editor)
            "Successfully read from $file"
        } catch (ex: Throwable) {
            ex.message?.let { "Failure: $it" } ?: "Unknown error"
        }
    }
}