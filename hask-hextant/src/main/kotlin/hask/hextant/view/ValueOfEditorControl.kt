/**
 *@author Nikolaus Knop
 */

package hask.hextant.view

import bundles.Bundle
import hask.hextant.editor.ReferenceCompleter
import hask.hextant.editor.ValueOfEditor
import hextant.core.view.AbstractTokenEditorControl
import javafx.css.PseudoClass

class ValueOfEditorControl(editor: ValueOfEditor, args: Bundle) : AbstractTokenEditorControl(editor, args), ValueOfEditorView {
    init {
        arguments[COMPLETER] = ReferenceCompleter
        root.styleClass.add("reference")
        editor.addView(this)
    }

    override fun displayHighlighting(highlighting: Boolean) {
        pseudoClassStateChanged(HIGHLIGHT, highlighting)
    }

    companion object {
        private val HIGHLIGHT = PseudoClass.getPseudoClass("highlight")
    }
}