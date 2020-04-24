package hask.hextant.ti

import hask.hextant.ti.env.ReleasableNamer
import hextant.test.shouldEqual
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.*

object NamerSpec: Spek({
    given("a namer") {
        val namer = ReleasableNamer()
        on("requesting a fresh name") {
            it("should return a0") {
                namer.freshName() shouldEqual "a0"
            }
        }
        on("requesting another fresh name") {
            it("should return b0") {
                namer.freshName() shouldEqual "b0"
            }
        }
        on("releasing a name and then requesting a new one") {
            namer.release(namer.freshName())
            it("should return the released name") {
                namer.freshName() shouldEqual "c0"
            }
        }
        on("requesting so many names that one character doesn't fit") {
            while (!namer.freshName().startsWith("z"))
            it("should start from 'a' but increment the counter") {
                namer.freshName() shouldEqual "a1"
            }
        }
    }
})