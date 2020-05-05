package hask.core.rt

import hask.core.ast.Expr
import hask.core.rt.Thunk.ThunkState.Evaluated
import hask.core.rt.Thunk.ThunkState.Unevaluated

class Thunk private constructor(private var state: ThunkState) {
    fun force(): NormalForm = when (val st = state) {
        is Unevaluated -> st.expr.force(st.frame).also { state = Evaluated(it) }
        is Evaluated   -> st.value
    }

    override fun toString(): String = when (val st = state) {
        is Unevaluated -> "unevaluated ${st.expr}}"
        is Evaluated   -> "evaluated ${st.value}"
    }

    private sealed class ThunkState {
        data class Unevaluated(val frame: StackFrame, val expr: Expr) : ThunkState()

        data class Evaluated(val value: NormalForm) : ThunkState()
    }

    companion object {
        fun lazy(frame: StackFrame, expr: Expr) =
            Thunk(Unevaluated(frame, expr))

        fun strict(value: NormalForm) =
            Thunk(Evaluated(value))
    }
}