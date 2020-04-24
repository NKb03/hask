/**
 *@author Nikolaus Knop
 */

package hask.core.parse

class CharInput private constructor(
    private val chars: CharSequence,
    private val index: Int,
    private val line: Int,
    private val column: Int
) : Input<Char> {
    override fun next(): Char = if (hasNext()) chars[index] else throw ParserError("No more chars")

    override fun hasNext(): Boolean = index < chars.length

    override fun consume(): Input<Char> {
        val consumed = chars[index]
        return if (consumed == '\n') CharInput(chars, index + 1, line + 1, 1)
        else CharInput(chars, index + 1, line, column + 1)
    }

    override fun location(): Location = LCLocation(line, column)

    override fun toString(): String = "$chars at ${location()}"

    companion object {
        fun from(string: String) = CharInput(string, 0, 1, 1)
    }
}
