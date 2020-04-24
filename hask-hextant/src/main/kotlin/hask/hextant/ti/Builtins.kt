package hask.hextant.ti

import hask.core.ast.Builtin
import hask.core.type.Type.ParameterizedADT

object Builtins {
    val BoolT = ParameterizedADT(Builtin.Boolean, emptyList())
}