/**
 *@author Nikolaus Knop
 */

package hask.core.parse

class TokenInput<T: IToken> private constructor(private val tokens: List<T>, private val index: Int): Input<T> {
    override fun next(): T = if (hasNext()) tokens[index] else throw ParserError("No more tokens available")

    override fun hasNext(): Boolean = index < tokens.size

    override fun consume(): Input<T> = TokenInput(tokens, index)

    override fun location(): Location = tokens[index].location

    companion object {
        fun <T : IToken> from(tokens: List<T>) = TokenInput(tokens, 0)
    }
}