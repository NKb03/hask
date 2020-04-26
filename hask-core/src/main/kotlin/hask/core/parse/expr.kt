/**
 * @author Nikolaus Knop
 */

package hask.core.parse

import hask.core.ast.Expr
import hask.core.ast.Expr.Binding

val single: Parser<Token, Expr> = doParse {
    when (val tok = next()) {
        is Token.I  -> {
            consume()
            success(Expr.IntLiteral(tok.value))
        }
        is Token.Id -> {
            consume()
            success(Expr.ValueOf(tok.name))
        }
        is Token.LP -> {
            val e = expr()
            expect("')'") { it is Token.RP }
            success(e)
        }
        else        -> fail("$tok is not a single expression")
    }
}
val id: Parser<Token, String> = doParse {
    val n = next()
    if (n is Token.Id) {
        consume()
        success(n.name)
    } else fail("Expected identifier got $n")
}

val let: Parser<Token, Expr> = doParse {
    expect("'let'") { it is Token.Let }
    val bindings = mutableListOf<Binding>()
    while (true) {
        val name = id()
        expect("'='") { it is Token.Assign }
        val value = expr()
        bindings.add(Binding(name, value))
        if (next() !is Token.Comma) break
        consume()
    }
    expect("'in'") { it is Token.In }
    val body = expr()
    success(Expr.Let(bindings, body))
}

val lambda: Parser<Token, Expr> = doParse {
    expect("'lambda'") { it is Token.Lambda }
    val name = id()
    expect("'->'") { it is Token.Arrow }
    val body = expr()
    success(Expr.Lambda(name, body))
}

val `if`: Parser<Token, Expr> = doParse {
    expect("'if'") { it is Token.If }
    val cond = expr()
    expect("'then'") { it is Token.Then }
    val then = expr()
    expect("'else'") { it is Token.Else }
    val els = expr()
    success(Expr.If(cond, then, els))
}

val nonAppExpr: Parser<Token, Expr> = alt(let, lambda, `if`) { "Invalid expression" }

val apply: Parser<Token, Expr> = doParse {
    val left = expr()
    val right = single()
    success(Expr.Apply(left, right))
}

val expr: Parser<Token, Expr> = alt(single, nonAppExpr, apply) { "Invalid expression" }