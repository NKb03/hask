/**
 *@author Nikolaus Knop
 */

package hask.core.ast

sealed class Pattern {
    object Wildcard : Pattern()
    class Variable(val name: String) : Pattern()
    class Integer(val value: String) : Pattern()
    class Destructuring(val constructor: String, val components: List<Pattern>) : Pattern()

    override fun toString(): String = when (this) {
        Wildcard         -> "_"
        is Variable      -> name
        is Integer       -> value
        is Destructuring -> buildString {
            append(constructor)
            for (c in components) {
                if (c.isComposite()) append('(')
                append(c)
                if (c.isComposite()) append(')')
            }
        }
    }

    private fun isComposite() = this is Destructuring
}