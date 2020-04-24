/**
 * @author Nikolaus Knop
 */

package hask.core.parse

interface Parser<in T, out R> {
    fun parse(input: Input<T>): ParseResult<@UnsafeVariance T, R>
}