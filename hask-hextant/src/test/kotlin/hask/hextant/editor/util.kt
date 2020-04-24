/**
 * @author Nikolaus Knop
 */

package hask.hextant.editor

import hextant.core.editor.Expander
import reaktive.value.now

inline fun <reified R> Expander<*, *>.doExpand(text: String): R {
    doExpandTo(text)
    return editor.now as R
}

inline fun <reified R> Expander<*, *>.expand(text: String, then: R.() -> Unit) {
    doExpandTo(text)
    then(editor.now as R)
}

fun Expander<*, *>.doExpandTo(text: String) {
    setText(text)
    expand()
}