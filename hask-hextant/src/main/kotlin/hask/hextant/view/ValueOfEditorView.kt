/**
 * @author Nikolaus Knop
 */

package hask.hextant.view

import hextant.core.view.TokenEditorView

interface ValueOfEditorView : TokenEditorView {
    fun displayHighlighting(highlighting: Boolean)
}