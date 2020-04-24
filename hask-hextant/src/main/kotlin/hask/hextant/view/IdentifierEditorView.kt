/**
 * @author Nikolaus Knop
 */

package hask.hextant.view

import hextant.core.view.TokenEditorView

interface IdentifierEditorView : TokenEditorView {
    fun displayRefactoring(refactoring: Boolean)
}