/**
 * @author Nikolaus Knop
 */

package hask.core.parse

sealed class Token: IToken {
    data class Let(override val location: Location) : Token()
    data class Lambda(override val location: Location) : Token()
    data class If(override val location: Location) : Token()
    data class Then(override val location: Location) : Token()
    data class Else(override val location: Location) : Token()
    data class In(override val location: Location) : Token()
    data class Assign(override val location: Location) : Token()
    data class Arrow(override val location: Location) : Token()
    data class LP(override val location: Location) : Token()
    data class RP(override val location: Location) : Token()
    data class I(val value: Int, override val location: Location) : Token() {
        override fun toString(): String {
            return super.toString()
        }
    }

    data class Id(val name: String, override val location: Location) : Token() {
        override fun toString(): String {
            return super.toString()
        }
    }

    override fun toString(): String = when (this) {
        is Let    -> "let"
        is Lambda -> "lambda"
        is If     -> "if"
        is Then   -> "then"
        is Else   -> "else"
        is In     -> "in"
        is Assign -> "="
        is Arrow  -> "->"
        is LP     -> "("
        is RP     -> ")"
        is I   -> value.toString()
        is Id  -> name
    }
}

val token = doParse<Char, Token> {
    whitespace()
    val chars = takeUntil(Char::isWhitespace).parse()
    whitespace()
    val str = chars.joinToString("")
    val tok = when (str) {
        "let"          -> Token.Let(location())
        "\\", "lambda" -> Token.Lambda(location())
        "if"           -> Token.If(location())
        "then"         -> Token.Then(location())
        "else"         -> Token.Else(location())
        "In"           -> Token.In(location())
        "="            -> Token.Assign(location())
        "->"           -> Token.Arrow(location())
        "("            -> Token.LP(location())
        ")"            -> Token.RP(location())
        else           -> when {
            str.toIntOrNull() != null     -> Token.I(str.toInt(), location())
            str.matches(IDENTIFIER_REGEX) -> Token.Id(str, location())
            else                          -> null
        }
    }
    if (tok != null) success(tok) else fail("Invalid token $str")
}

val tokens = forceConsumeAll(multiple(token))

fun main() {
    val input = "if eq 1 2 then lambda x -> x else lambda x -> 1"
    val result = tokens.parse(CharInput.from(input))
    val e = result.flatMap { _, tokens ->
        val tokenInput = TokenInput.from(tokens)
        expr.parse(tokenInput)
    }
    println(e)
}

val IDENTIFIER_REGEX = Regex("[a-zA-Z][a-zA-Z0-9]*")