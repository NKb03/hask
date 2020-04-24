/**
 *@author Nikolaus Knop
 */

package hask.hextant.view

import hask.hextant.editor.ReferenceCompleter
import hask.hextant.editor.ValueOfEditor
import hextant.bundle.Bundle
import hextant.core.view.AbstractTokenEditorControl
import javafx.css.PseudoClass

class ValueOfEditorControl(editor: ValueOfEditor, args: Bundle) : AbstractTokenEditorControl(editor, args, ReferenceCompleter),
                                                                  ValueOfEditorView {
    init {
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