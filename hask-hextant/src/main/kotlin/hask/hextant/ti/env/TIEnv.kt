/**
 * @author Nikolaus Knop
 */

package hask.hextant.ti.env

import hask.core.type.TypeScheme
import hask.core.type.Type
import reaktive.set.ReactiveSet
import reaktive.value.ReactiveValue

interface TIEnv {
    fun resolve(name: String): ReactiveValue<Type?>

    fun generalize(t: Type): ReactiveValue<TypeScheme>

    val freeTypeVars: ReactiveSet<String>

    val now: Map<String, TypeScheme>
}