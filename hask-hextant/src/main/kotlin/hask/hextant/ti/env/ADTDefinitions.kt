/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti.env

import bundles.PublicProperty
import bundles.property
import hask.core.ast.*
import hask.core.type.Type.ParameterizedADT
import hask.core.type.Type.Var
import hask.core.type.TypeScheme
import hask.core.type.functionType
import hask.hextant.editor.ADTDefEditor
import reaktive.Observer
import reaktive.collection.binding.contains
import reaktive.list.ReactiveList
import reaktive.list.binding.values
import reaktive.map.bindings.get
import reaktive.map.reactiveMap
import reaktive.set.ReactiveSet
import reaktive.set.asSet
import reaktive.set.binding.mapNotNull
import reaktive.set.binding.values
import reaktive.value.ReactiveValue
import reaktive.value.binding.flatMap
import reaktive.value.reactiveValue
import validated.*
import validated.reaktive.ReactiveValidated
import kotlin.collections.Collection
import kotlin.collections.map
import kotlin.collections.mutableSetOf
import kotlin.collections.set

class ADTDefinitions(editors: ReactiveList<ADTDefEditor>) {
    private val results = editors.map { it.result }.values()
    val abstractDataTypes: ReactiveSet<ADT> =
        editors.asSet().mapNotNull { def -> def.adt.result }.values().mapNotNull { it.orNull() }
    private val availableNames = abstractDataTypes.map { it.name }
    private val constructorInfo = reactiveMap<String, Pair<ADT, ADTConstructor>>()
    private val constructors = mutableSetOf<ADTConstructor>()

    fun isResolved(name: ReactiveValidated<String>) = name.flatMap { n ->
        n.fold(
            onValid = { availableNames.contains(it) },
            onInvalid = { reactiveValue(true) }
        )
    }

    fun constructors(): Collection<ADTConstructor> = constructors

    fun bindConstructors(env: TIEnv): Observer {
        for (def in results.now) def.ifValid { addDefinition(env, it) }
        return results.observeList { ch ->
            when {
                ch.wasReplaced -> {
                    ch.removed.ifValid { removeDefinition(env, it) }
                    ch.added.ifValid { addDefinition(env, it) }
                }
                ch.wasAdded    -> ch.added.ifValid { addDefinition(env, it) }
                ch.wasRemoved  -> ch.removed.ifValid { removeDefinition(env, it) }
            }
        }
    }

    fun getInfo(constructor: String): ReactiveValue<Pair<ADT, ADTConstructor>?> = constructorInfo[constructor]

    private fun addDefinition(env: TIEnv, def: ADTDef) {
        for (cstr in def.constructors) {
            if (env.declaredType(cstr.name) != null) continue
            val scheme = getTypeScheme(def.adt, cstr)
            env.bind(cstr.name, scheme)
            constructorInfo.now[cstr.name] = def.adt to cstr
            constructors.add(cstr)
        }
    }

    private fun removeDefinition(env: TIEnv, def: ADTDef) {
        for (cstr in def.constructors) {
            val scheme = getTypeScheme(def.adt, cstr)
            if (env.declaredType(cstr.name) == scheme) env.unbind(cstr.name)
            constructorInfo.now.remove(cstr.name)
            constructors.remove(cstr)
        }
    }

    private fun getTypeScheme(adt: ADT, cstr: ADTConstructor): TypeScheme {
        val params = adt.typeParameters
        val t = ParameterizedADT(adt.name, adt.typeParameters.map { Var(it) })
        return TypeScheme(params, functionType(cstr.parameters, t))
    }

    companion object : PublicProperty<ADTDefinitions> by property("ast definitions")
}