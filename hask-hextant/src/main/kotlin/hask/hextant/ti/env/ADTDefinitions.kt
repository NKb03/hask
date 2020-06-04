/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti.env

import bundles.SimpleProperty
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
import reaktive.set.ReactiveSet
import reaktive.set.asSet
import reaktive.set.binding.mapNotNull
import reaktive.set.binding.values
import reaktive.value.binding.flatMap
import reaktive.value.reactiveValue
import validated.*
import validated.reaktive.ReactiveValidated

class ADTDefinitions(editors: ReactiveList<ADTDefEditor>) {
    private val results = editors.map { it.result }.values()
    val abstractDataTypes: ReactiveSet<ADT> =
        editors.asSet().mapNotNull { def -> def.adt.result }.values().mapNotNull { it.orNull() }
    private val availableNames = abstractDataTypes.map { it.name }

    fun isResolved(name: ReactiveValidated<String>) = name.flatMap { n ->
        n.fold(
            onValid = { availableNames.contains(it) },
            onInvalid = { reactiveValue(true) }
        )
    }

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

    private fun addDefinition(env: TIEnv, def: ADTDef) {
        for (cstr in def.constructors) {
            if (env.declaredType(cstr.name) != null) continue
            val scheme = getTypeScheme(def.adt, cstr)
            env.bind(cstr.name, scheme)
        }
    }

    private fun removeDefinition(env: TIEnv, def: ADTDef) {
        for (cstr in def.constructors) {
            val scheme = getTypeScheme(def.adt, cstr)
            if (env.declaredType(cstr.name) == scheme) env.unbind(cstr.name)
        }
    }

    private fun getTypeScheme(adt: ADT, cstr: ADTConstructor): TypeScheme {
        val params = adt.typeParameters
        val t = ParameterizedADT(adt.name, adt.typeParameters.map { Var(it) })
        return TypeScheme(params, functionType(cstr.parameters, t))
    }

    companion object : SimpleProperty<ADTDefinitions>("ast definitions")
}