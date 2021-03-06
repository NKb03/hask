/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti

import hask.core.type.Type
import hask.core.type.Type.Var
import hask.core.type.TypeScheme
import hask.hextant.ti.env.TIContext
import reaktive.value.ReactiveValue
import reaktive.value.binding.flatMap
import reaktive.value.forEach
import validated.Validated
import validated.orNull
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

class LetTypeInference(
    context: TIContext,
    private val bindings: () -> List<Pair<Validated<String>, TypeInference>>,
    private val dependencyGraph: DependencyGraph,
    private val body: TypeInference
) : AbstractTypeInference(context) {
    init {
        dependsOn(dependencyGraph.invalidated)
    }

    override fun onReset() {
        for (b in bindings()) {
            val e = env(b)
            e.clear()
        }
        env(body).clear()
    }

    private fun env(b: Pair<Validated<String>, TypeInference>) = env(b.second)

    private fun env(inf: TypeInference) = inf.context.env

    override fun doRecompute() {
        val vertices = bindings()
        val ts = dependencyGraph.topologicallySortedSCCs()
        val env = mutableMapOf<String, ReactiveValue<TypeScheme>>()
        val typeVars = List(vertices.size) { Var(freshName()) }
        computePrincipalTypes(ts, vertices, env, typeVars)
        addBackReferences(ts, vertices, typeVars)
        addEnvToBody(env)
    }

    private fun computePrincipalTypes(
        ts: List<Collection<Int>>,
        vertices: List<Pair<Validated<String>, TypeInference>>,
        env: MutableMap<String, ReactiveValue<TypeScheme>>,
        typeVars: List<Var>
    ) {
        for (comp in ts) {
            for (i in comp) {
                val e = env(vertices[i])
                for ((name, type) in env) {
                    val o = type.forEach { t -> e.bind(name, t) }
                    addObserver(o, killOnRecompute = true)
                }
                for (j in comp) {
                    val name = vertices[j].first.orNull() ?: continue
                    e.bind(name, typeVars[j])
                }
            }
            for (i in comp) {
                val n = vertices[i].first.orNull() ?: continue
                val inf = vertices[i].second
                val defTypeVar = typeVars[i]
                bind(inf.type, defTypeVar)
                env[n] = inf.type.flatMap { t ->
                    inf.context.unificator.substitute(t).flatMap { inf.context.env.generalize(it) }
                }
            }
        }
    }

    private fun addBackReferences(
        ts: List<Collection<Int>>,
        vertices: List<Pair<Validated<String>, TypeInference>>,
        typeVars: List<Var>
    ) {
        for (i in ts.indices) {
            for (j in 0 until i) {
                for (u in ts[i]) {
                    val name = vertices[u].first.orNull() ?: continue
                    val type = typeVars[u]
                    for (v in ts[j]) {
                        val e = env(vertices[v])
                        e.bind(name, type)
                    }
                }
            }
        }
    }

    private fun addEnvToBody(env: Map<String, ReactiveValue<TypeScheme>>) {
        for ((name, type) in env) {
            val e = body.context.env
            val o = type.forEach { t -> e.bind(name, t) }
            addObserver(o, killOnRecompute = true)
        }
    }

    override fun children(): Collection<TypeInference> = bindings().map { it.second } + body

    override val type: ReactiveValue<Type> get() = body.type
}