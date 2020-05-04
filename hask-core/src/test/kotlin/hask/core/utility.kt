/**
 * @author Nikolaus Knop
 */

package hask.core

import hask.core.type.*
import hask.core.type.Type.Var
import kotlin.reflect.KClass

infix fun <T> T.shouldEqual(expected: T) = assert(this == expected) { "Expected $expected, but got $this" }

infix fun Any.shouldPrint(string: String) = toString().shouldEqual(string)

infix fun TypeScheme.shouldMatch(expected: TypeScheme) {
    assert(match(this, expected)) { "Expected $expected, but got $this" }
}

infix fun TypeScheme.shouldMatch(expected: String) {
    val t = parseType(expected).ifFailure { f -> throw AssertionError(f.message) }.result
    shouldMatch(t.generalize(emptySet()))
}

fun <R> (() -> R).shouldThrow(exceptionCls: KClass<out Throwable>) {
    try {
        this()
        throw AssertionError("Did not throw")
    } catch (e: Throwable) {
        assert(exceptionCls.java.isInstance(e)) {
            "Did throw ${e.javaClass.simpleName} instead of ${exceptionCls.java.simpleName}"
        }
    }
}

private fun match(ts1: TypeScheme, ts2: TypeScheme): Boolean {
    val subst = ts2.names.zip(ts1.names.map { Var(it) }).toMap()
    return TypeScheme(ts1.names, ts2.body.apply(subst)) == ts1
}