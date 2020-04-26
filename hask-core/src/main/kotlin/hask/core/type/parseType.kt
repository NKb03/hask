package hask.core.type

import hask.core.type.ParseResult.Failure
import hask.core.type.ParseResult.Success
import hask.core.type.Type.*

sealed class ParseResult {
    data class Success(val result: Type, val rest: String) : ParseResult()

    data class Failure(val message: String) : ParseResult()
}

inline fun ParseResult.ifFailure(def: (Failure) -> Success): Success = when(this) {
    is Success -> this
    is Failure -> def(this)
}

fun ParseResult.orNull() = when (this) {
    is Success -> result
    is Failure -> null
}

val String.head get() = first()

val String.tail get() = drop(1)

private fun parseType(str: String, parentheses: Int): ParseResult {
    if (str.isEmpty()) Failure("Expected type")
    val (type, rest) =
        if (str.head == '(') parseType(str.tail, parentheses + 1)
        else {
            val name = str.takeWhile { it.isLetter() }
            val type = if (name == "int") INT else Var(name)
            Success(type, str.drop(name.length))
        }.ifFailure { return it }
    return when {
        rest.isEmpty() -> {
            require(parentheses == 0)
            Success(type, "")
        }
        rest.startsWith(')')  -> {
            require(parentheses > 0)
            Success(type, rest.drop(1))
        }
        rest.startsWith("->") -> {
            val (resultType, rest2) = parseType(rest.drop(2), parentheses).ifFailure { return it }
            Success(Func(type, resultType), rest2)
        }
        else -> Failure("Cannot parse $rest after type")
    }
}

fun parseType(literal: String) = parseType(removeWhitespace(literal), 0)

private fun removeWhitespace(s: String) = buildString {
    for (c in s) if (c != ' ') this.append(c)
}