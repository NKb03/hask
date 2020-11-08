/**
 * @author Nikolaus Knop
 */

package hask.hextant.context

import bundles.Permission

sealed class HaskInternal : Permission("hask.internal") {
    internal companion object : HaskInternal()
}