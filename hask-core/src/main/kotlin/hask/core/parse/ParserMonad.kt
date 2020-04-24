/**
 *@author Nikolaus Knop
 */

package hask.core.parse

import hask.core.parse.ParseResult.Failure
import hask.core.parse.ParseResult.Success

class Terminate(val failure: Failure) : Throwable()

class ParserMonad<T>(private var input: Input<T>) {

    fun terminate(failure: Failure): Nothing = throw Terminate(failure)

    fun terminate(message: String): Nothing = terminate(Failure(message, input.location()))

    operator fun <R> Parser<T, R>.invoke(): R {
        val result = parse(input)
        val success = result.ifErr(::terminate)
        input = success.rest
        return success.value
    }

    fun hasNext() = input.hasNext()

    fun next(): T = if (input.hasNext()) input.next() else terminate("Unexpected end of input")

    fun consume() {
        input = input.consume()
    }

    fun location() = input.location()

    fun fail(message: String) = Failure(message, input.location())

    fun <R> success(value: R) = Success(value, input)

    fun <R> Parser<T, R>.parse() = invoke()

    inline fun expect(description: String, predicate: (T) -> Boolean) {
        val n = next()
        if (!predicate(n)) terminate("Expected $description got $n")
        else consume()
    }
}

inline fun <T, R> doParse(crossinline body: ParserMonad<T>.() -> ParseResult<T, R>): Parser<T, R> = parser { input ->
    try {
        val monad = ParserMonad(input)
        monad.body()
    } catch (t: Terminate) {
        t.failure
    }
}