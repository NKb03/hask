/**
 *@author Nikolaus Knop
 */

package hask.core.parse

import hask.core.parse.ParseResult.Failure
import hask.core.parse.ParseResult.Success

sealed class ParseResult<out T, out R> {
    data class Success<out T, out R>(val value: R, val rest: Input<T>): ParseResult<T, R>()

    data class Failure(val message: String, val location: Location): ParseResult<Nothing, Nothing>()
}

fun <T, R, F> ParseResult<T, R>.map(f: (R) -> F) = when (this) {
    is Success -> Success(f(value), rest)
    is Failure -> this
}

fun <T, S, R, F> ParseResult<T, R>.flatMap(f: (Input<T>, R) -> ParseResult<S, F>) = when (this) {
    is Success -> f(rest, value)
    is Failure -> this
}

fun <T, R> ParseResult<T, R>.ifErr(onFailure: (Failure) -> Success<T, R>): Success<T, R> = when (this) {
    is Success -> this
    is Failure -> onFailure(this)
}