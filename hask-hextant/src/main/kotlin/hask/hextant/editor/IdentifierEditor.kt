/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.hextant.view.IdentifierEditorView
import hextant.*
import hextant.core.editor.TokenEditor
import reaktive.value.now

class IdentifierEditor(context: Context) : TokenEditor<String, IdentifierEditorView>(context) {
    constructor(context: Context, text: String): this(context)  {
        setText(text)
    }

    override fun compile(token: String): CompileResult<String> =
        token.takeIf { it.matches(IDENTIFIER_REGEX) }.okOrErr { "Invalid identifier $token" }

    private var refactoring = false

    fun startRefactoring() {
        refactoring = true
        views { displayRefactoring(true) }
    }

    fun stopRefactoring() {
        refactoring = false
        views { displayRefactoring(false) }
    }

    private val refactor = result.observe { _, old, new ->
        if (refactoring && new is Ok && old is Ok) {
            withAllReferences(old.value) { refs ->
                refs.forEach { ref ->
                    ref.setText(new.value)
                }
            }
        }
    }

    private fun withAllReferences(
        name: String,
        block: (List<ValueOfEditor>) -> Unit
    ) {
        val parent = parent
        val references = mutableListOf<ValueOfEditor>()
        when (parent) {
            is LambdaEditor -> parent.body.collectReferences(name, references)
            is LetEditor    -> parent.body.collectReferences(name, references)
            else            -> return
        }
        block(references)
    }

    fun highlightReferences() {
        val name = result.now.ifErr { return }
        withAllReferences(name) { refs -> refs.forEach { ref -> ref.setHighlighting(true) } }
    }

    fun stopHighlightingReferences() {
        val name = result.now.ifErr { return }
        withAllReferences(name) { refs -> refs.forEach { ref -> ref.setHighlighting(false) } }
    }

    companion object {
        val IDENTIFIER_REGEX = Regex("[a-zA-Z][a-zA-Z0-9_]*")
    }
}