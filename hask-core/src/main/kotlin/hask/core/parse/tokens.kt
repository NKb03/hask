/**
 * @author Nikolaus Knop
 */

package hask.core.parse

sealed class Token : IToken {
    class Let(override val location: Location) : Token()
    class Lambda(override val location: Location) : Token()
    class If(override val location: Location) : Token()
    class Then(override val location: Location) : Token()
    class Else(override val location: Location) : Token()
    class In(override val location: Location) : Token()
    class Assign(override val location: Location) : Token()
    class Arrow(override val location: Location) : Token()
    class LP(override val location: Location) : Token()
    class RP(override val location: Location) : Token()
    class I(val value: Int, override val location: Location) : Token()
    class Comma(override val location: Location) : Token()
    class Id(val name: String, override val location: Location) : Token()

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
        is I      -> value.toString()
        is Id     -> name
        is Comma  -> ", "
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
        "in"           -> Token.In(location())
        "="            -> Token.Assign(location())
        "->"           -> Token.Arrow(location())
        "("            -> Token.LP(location())
        ")"            -> Token.RP(location())
        ","            -> Token.Comma(location())
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