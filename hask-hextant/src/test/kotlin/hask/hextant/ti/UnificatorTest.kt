/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti

import com.nhaarman.mockitokotlin2.mock
import hask.core.type.Type
import hask.core.type.Type.Var
import hask.hextant.t
import hask.hextant.ti.unify.*
import org.junit.jupiter.api.Test

class UnificatorTest {
    private class TestingFlow(private val unificator: Unificator) {
        private val ti = mock<TypeInference>()

        private val constraints = mutableListOf<Constraint>()

        fun bind(t1: Type, t2: Type) {
            val constraint = Constraint(t1, t2, ti)
            unificator.add(constraint)
            constraints.add(constraint)
            checkSubstitutions()
        }

        fun unbind(t1: Type, t2: Type) {
            val constraint = Constraint(t1, t2, ti)
            unificator.remove(constraint)
            constraints.remove(constraint)
            checkSubstitutions()
        }

        private fun checkSubstitutions() {
            val expected = unify(constraints)
            val actual = unificator.substitutions()
            val errors = mutableListOf<String>()
            for ((n, t) in expected) {
                val actualType = actual[n]
                if (actualType == null) {
                    if (t !is Var || actual[t.name] != Var(n)) {
                        errors.add("Substitution for $n misses, expected $n = $t")
                    }
                    continue
                }
                if (actualType != t) {
                    errors.add("Substitution for $n is wrong. Expected $t, got $actualType")
                }
            }
            for ((n, t) in actual - expected.keys) {
                if (t !is Var || expected[t.name] != Var(n)) {
                    errors.add("Unexpected substitution $n = $t")
                }
            }
            if (errors.isNotEmpty()) {
                errors.add(0, "\nExpected: $expected\nActual: $actual")
                throw AssertionError(errors.joinToString("\n"))
            } else {
                println(actual)
            }
        }

        infix fun String.bind(t2: String) = bind(t, t2.t)

        infix fun String.unbind(t2: String) = unbind(t, t2.t)
    }

    private fun test(block: TestingFlow.() -> Unit) {
        TestingFlow(SimpleUnificator()).block()
    }

    @Test
    fun `simple transitive constraints`() {
        test {
            "a" bind "b"
            "b" bind "c"
        }
    }

    @Test
    fun `bind simple constraint`() {
        test {
            "b" bind "int -> int"
        }
    }

    @Test
    fun `bind and unbind simple constraint`() {
        test {
            "b" bind "int -> int"
            "b" unbind "int -> int"
        }
    }

    @Test
    fun `constraints for let id = lambda x - x in (id id) 1`() {
        test {
            "b -> b" bind "(c -> c) -> d"
            "d" bind "int -> e"
        }
    }

    @Test
    fun `bind and unbind constraints for let id = lambda x - x in (id id) 1`() {
        test {
            "b -> b" bind "(c -> c) -> d"
            "d" bind "int -> e"
            "d" unbind "int -> e"
            "b -> b" unbind "(c -> c) -> d"
        }
    }

    @Test
    fun `bind two unrelated constraint systems`() {
        test {
            "a -> a" bind "b -> c -> int"
            "d -> e" bind "e -> d"
        }
    }

    @Test
    fun `bind and unbind two unrelated constraint systems`() {
        test {
            "a -> a" bind "b -> c -> int"
            "d -> e" bind "e -> d"
            "d -> e" unbind "e -> d"
            "a -> a" unbind "b -> c -> int"
        }
    }

    @Test
    fun test() {
        test {
            "j" bind "int -> n"
            "int -> int -> int" bind "z -> l"
            "l" bind "n -> k"
            "j" bind "int -> z"
        }
    }
}