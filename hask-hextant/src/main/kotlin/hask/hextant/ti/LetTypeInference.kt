/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti

import hask.core.type.Type
import hask.core.type.Type.Var
import hask.core.type.TypeScheme
import hask.hextant.ti.env.TIContext
import hask.hextant.ti.unify.ConstraintsHolder
import hask.hextant.ti.unify.bind
import hextant.*
import reaktive.Observer
import reaktive.event.subscribe
import reaktive.value.*
import reaktive.value.binding.flatMap
import reaktive.value.binding.map

class LetTypeInference(
    context: TIContext,
    private val bindings: () -> List<Pair<CompileResult<String>, TypeInference>>,
    private val dependencyGraph: DependencyGraph,
    private val bodyType: TypeInference,
    holder: ConstraintsHolder
) : AbstractTypeInference(context, holder) {
    private val observers = mutableListOf<Observer>()
    private val graphSubscription = dependencyGraph.invalidated.subscribe { _ -> recompute() }
    private val usedVariables = mutableSetOf<String>()

    init {
        addBindings(bindings())
    }

    private fun clear(bindings: List<Pair<CompileResult<String>, TypeInference>>) {
        for (it in observers) it.kill()
        observers.clear()
        holder.clearConstraints()
        clearErrors()
        for (b in bindings) {
            val e = env(b)
            e.clear()
        }
        env(bodyType).clear()
        for (n in usedVariables) context.namer.release(n)
        usedVariables.clear()
    }

    private fun env(b: Pair<CompileResult<String>, TypeInference>) = env(b.second)

    private fun env(inf: TypeInference) = inf.context.env

    private fun recompute() {
        val b = bindings()
        clear(b)
        addBindings(b)
    }

    private fun addBindings(vertices: List<Pair<CompileResult<String>, TypeInference>>) {
        val ts = dependencyGraph.topologicallySortedSCCs()
        val env = mutableMapOf<String, ReactiveValue<CompileResult<TypeScheme>>>()
        val typeVars = List(vertices.size) { freshTypeVar() }
        computePrincipalTypes(ts, vertices, env, typeVars)
        addBackReferences(ts, vertices, typeVars)
        addEnvToBody(env)
    }

    private fun computePrincipalTypes(
        ts: List<Collection<Int>>,
        vertices: List<Pair<CompileResult<String>, TypeInference>>,
        env: MutableMap<String, ReactiveValue<CompileResult<TypeScheme>>>,
        typeVars: List<Var>
    ) {
        for (comp in ts) {
            for (i in comp) {
                val e = env(vertices[i])
                for ((name, type) in env) {
                    val o = type.forEach { t -> e.bind(name, t) }
                    observers.add(o)
                }
                for (j in comp) {
                    val name = vertices[j].first.orNull() ?: continue
                    e.bind(name, ok(TypeScheme(emptyList(), typeVars[j])))
                }
            }
            for (i in comp) {
                val n = vertices[i].first.orNull() ?: continue
                val inf = vertices[i].second
                val defTypeVar = typeVars[i]
                holder.bind(reactiveValue(ok(defTypeVar)), inf.type, this)
                env[n] = inf.type.flatMap { t ->
                    if (t is Ok) context.unificator.substitute(t.value).flatMap { context.env.generalize(it) }
                        .map { ok(it) }
                    else reactiveValue(t.castError<Type, TypeScheme>())
                }
            }
        }
    }

    private fun addBackReferences(
        ts: List<Collection<Int>>,
        vertices: List<Pair<CompileResult<String>, TypeInference>>,
        typeVars: List<Var>
    ) {
        for (i in ts.indices) {
            for (j in 0 until i) {
                for (u in ts[i]) {
                    val name = vertices[u].first.orNull() ?: continue
                    val type = typeVars[u]
                    for (v in ts[j]) {
                        val e = env(vertices[v])
                        e.bind(name, ok(TypeScheme(emptyList(), type)))
                    }
                }
            }
        }
    }

    private fun addEnvToBody(env: Map<String, ReactiveValue<CompileResult<TypeScheme>>>) {
        for ((name, type) in env) {
            val e = bodyType.context.env
            val o = type.forEach { t -> e.bind(name, t) }
            observers.add(o)
        }
    }

    private fun freshTypeVar(): Var {
        val n = context.namer.freshName()
        usedVariables.add(n)
        return Var(n)
    }

    override fun dispose() {
        super.dispose()
        val b = bindings()
        clear(b)
        graphSubscription.cancel()
        bodyType.dispose()
        b.forEach { it.second.dispose() }
    }

    override val type get() = bodyType.type
}