/**
 *@author Nikolaus Knop
 */

package hask.core.parse

class LCLocation(val line: Int, val column: Int): Location {
    override fun toString(): String = "$line:$column"
}