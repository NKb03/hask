/**
 * @author Nikolaus Knop
 */

package hask.core.parse

import hask.core.parse.ParseResult.Failure
import hask.core.parse.ParseResult.Success

fun <T, R> parser(function: (Input<T>) -> ParseResult<T, R>) = object : Parser<T, R> {
    override fun parse(input: Input<T>): ParseResult<T, R> = function(input)
}

inline fun <T> takeWhile(crossinline predicate: (T) -> Boolean): Parser<T, List<T>> = doParse {
    val ts = mutableListOf<T>()
    while (hasNext()) {
        val t = next()
        if (predicate(t)) {
            ts.add(t)
            consume()
        } else break
    }
    success(ts)
}

fun <T, R> alt(vararg parsers: Parser<T, R>, failure: () -> String): Parser<T, R> = parser { input ->
    for (p in parsers) {
        val res = p.parse(input)
        if (res is Success) return@parser res
    }
    Failure(failure(), input.location())
}

val whitespace = takeWhile<Char> { it.isWhitespace() }

inline fun <T> takeUntil(crossinline predicate: (T) -> Boolean) = takeWhile<T> { !(predicate(it)) }

fun takeUntil(char: Char) = takeUntil<Char> { it == char }

fun <T, R, F> Parser<T, R>.map(f: (R) -> F) = parser<T, F> { parse(it).map(f) }

fun <T, R, F> Parser<T, R>.flatMap(f: (Input<T>, R) -> ParseResult<T, F>) = parser<T, F> { parse(it).flatMap(f) }

fun <T, R> multiple(p: Parser<T, R>): Parser<T, List<R>> = parser {
    val rs = mutableListOf<R>()
    var input = it
    loop@ while (true) {
        when (val result = p.parse(input)) {
            is Success -> {
                input = result.rest
                rs.add(result.value)
            }
            is Failure -> break@loop
        }
    }
    Success(rs, input)
}

fun <T, R> forceConsumeAll(p: Parser<T, R>): Parser<T, R> = parser { input ->
    when (val result = p.parse(input)) {
        is Failure -> result
        is Success ->
            if (!result.rest.hasNext()) result
            else Failure("Could not consume all tokens", result.rest.location())
    }
}