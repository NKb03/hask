/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti.env

import bundles.SimpleProperty
import reaktive.collection.binding.contains
import reaktive.set.ReactiveSet

class ADTDefinitionEnv(private val typeParameters: ReactiveSet<String>) {
    val availableTypes get() = typeParameters.now

    fun isResolved(name: String) = typeParameters.contains(name)

    companion object : SimpleProperty<ADTDefinitionEnv>("adt definition env")
}