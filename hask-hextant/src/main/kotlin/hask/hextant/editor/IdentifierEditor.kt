/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.hextant.view.IdentifierEditorView
import hextant.context.Context
import hextant.core.editor.TokenEditor
import reaktive.value.now
import validated.*
import validated.Validated.Valid

class IdentifierEditor(context: Context, text: String) : TokenEditor<String, IdentifierEditorView>(context, text) {
    constructor(context: Context): this(context, "")

    override fun compile(token: String): Validated<String> = valid(token)

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
        if (refactoring && new is Valid && old is Valid) {
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
        val name = result.now.ifInvalid { return }
        withAllReferences(name) { refs -> refs.forEach { ref -> ref.setHighlighting(true) } }
    }

    fun stopHighlightingReferences() {
        val name = result.now.ifInvalid { return }
        withAllReferences(name) { refs -> refs.forEach { ref -> ref.setHighlighting(false) } }
    }
}