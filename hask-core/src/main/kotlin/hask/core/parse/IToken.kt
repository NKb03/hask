/**
 * @author Nikolaus Knop
 */

package hask.core.parse

interface IToken {
    override fun toString(): String

    val location: Location
}