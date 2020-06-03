/**
 *@author Nikolaus Knop
 */

package hask.hextant.view

import bundles.Bundle
import hask.hextant.editor.IdentifierEditor
import hextant.core.view.AbstractTokenEditorControl
import hextant.fx.registerShortcuts
import hextant.fx.shortcut
import javafx.css.PseudoClass
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyCode.F2
import reaktive.value.forEach

class IdentifierEditorControl(editor: IdentifierEditor, args: Bundle) : AbstractTokenEditorControl(editor, args),
                                                                        IdentifierEditorView {
    private val highlighter = isSelected.forEach { selected ->
        if (selected) editor.highlightReferences()
        else editor.stopHighlightingReferences()
    }

    init {
        root.styleClass.add("identifier")
        editor.addView(this)
        registerShortcuts {
            on(shortcut(F2) {}) {
                editor.startRefactoring()
            }
            on(shortcut(ENTER) {}) {
                editor.stopRefactoring()
            }
        }
    }

    override fun displayRefactoring(refactoring: Boolean) {
        pseudoClassStateChanged(REFACTORING, refactoring)
    }

    companion object {
        private val REFACTORING = PseudoClass.getPseudoClass("refactoring")
    }
}