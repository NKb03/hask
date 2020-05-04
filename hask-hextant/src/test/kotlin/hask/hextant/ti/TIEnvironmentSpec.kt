/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti

import com.natpryce.hamkrest.should.shouldMatch
import hask.core.type.Type
import hask.core.type.Type.INT
import hask.hextant.ti.env.ReleasableNamer
import hask.hextant.ti.env.SimpleTIEnv
import hextant.Ok
import hextant.test.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.*
import reaktive.value.now

object TIEnvironmentSpec : Spek({
    given("an empty type inference environment") {
        val env = SimpleTIEnv(ReleasableNamer())
        it("should be empty") {
            env.now.entries shouldMatch isEmpty
        }
        val t1 = env.resolve("v1")
        on("resolving some variable") {
            it("should be null") {
                t1.now shouldBe `null`
            }
        }
        on("binding some variable") {
            env.bind("v1", Ok(INT.generalize(emptySet())))
            it("should set the resolved variable type") {
                t1.now shouldEqual Type.INT
            }
        }
        on("unbinding the variable") {
            env.unbind("v1")
            it("should set the resolved variable type to null") {
                t1.now shouldBe `null`
            }
        }
        given("a child environment") {
            val child = env.child()
            it("should have all the entries of the parent env") {
                env.bind("v1", Ok(Type.INT.generalize(emptySet())))
                child.now shouldEqual mapOf("v1" to Type.INT.generalize(emptySet()))
                child.resolve("v1").now shouldEqual Type.INT
            }
        }
    }
})