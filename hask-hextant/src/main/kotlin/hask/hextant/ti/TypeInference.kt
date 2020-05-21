/**
 * @author Nikolaus Knop
 */

package hask.hextant.ti

import hask.core.type.Type
import hask.hextant.ti.env.ErrorDisplay
import hask.hextant.ti.env.TIContext
import hextant.CompileResult
import reaktive.set.ReactiveSet
import reaktive.value.ReactiveValue

interface TypeInference : ErrorDisplay {
    val isActive: Boolean

    val context: TIContext

    val type: ReactiveValue<CompileResult<Type>>

    val errors: ReactiveSet<Pair<Type, Type>>

    fun activate()

    fun dispose()
}