/**
 * @author Nikolaus Knop
 */

package hask.hextant.ti.env

import hask.core.type.Type

interface ErrorDisplay {
    fun reportError(a: Type, b: Type)

    fun removeError(a: Type, b: Type)

    fun clearErrors()
}