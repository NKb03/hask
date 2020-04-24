/**
 * @author Nikolaus Knop
 */

package hask.hextant

import hask.core.type.Type
import hask.hextant.editor.type.ifFailure
import hask.hextant.editor.type.parseType
import hask.core.type.Type.*
import hask.hextant.ti.unify.Constraint
import hask.hextant.ti.unify.Unificator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import kotlin.random.Random

infix fun <T> T.shouldEqual(expected: T) {
    assertEquals(expected, this)
}

fun genRandomType(depth: Int, variablePool: List<String>): Type = when {
    depth <= 0 -> if (Random.nextInt(5) > 1) Var(variablePool.random()) else INT
    depth == 1 -> Func(genRandomType(0, variablePool), genRandomType(0, variablePool))
    else       -> Func(
        genRandomType(depth - Random.nextInt(1, depth), variablePool),
        genRandomType(depth - Random.nextInt(1, depth), variablePool)
    )
}



val String.t: Type get() = parseType(this).ifFailure { fail(it.message) }.result

fun Unificator.registerConstraints(constraints: List<Constraint>) {
    for (c in constraints) add(c)
}