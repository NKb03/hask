/**
 * @author Nikolaus Knop
 */

package hask.core

import kotlin.reflect.KClass

infix fun <T> T.shouldEqual(expected: T) = assert(this == expected) { "Expected $expected, but got $this" }

infix fun Any.shouldPrint(string: String) = toString().shouldEqual(string)

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