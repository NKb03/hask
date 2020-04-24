package hask.hextant.ti

import hask.core.type.Type
import hextant.force
import org.junit.jupiter.api.Assertions.assertEquals
import reaktive.value.now

fun TypeInference.assertType(type: Type) {
    val actual = this.type.now.force()
    val substituted = context.unificator.substituteNow(actual)
    val general = context.env.generalize(substituted).now
    assertEquals(type, general)
}