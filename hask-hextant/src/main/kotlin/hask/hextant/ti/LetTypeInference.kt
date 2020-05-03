/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti

import hask.core.type.SCCs
import hask.core.type.Type.Var
import hask.core.type.TypeScheme
import hask.hextant.ti.env.SimpleTIEnv
import hask.hextant.ti.env.TIContext
import hask.hextant.ti.unify.ConstraintsHolder
import hask.hextant.ti.unify.bind
import hextant.*
import reaktive.Observer
import reaktive.list.ReactiveList
import reaktive.list.observeEach
import reaktive.set.ReactiveSet
import reaktive.set.asSet
import reaktive.set.binding.values
import reaktive.value.*
import reaktive.value.binding.*

class LetTypeInference(
    context: TIContext,
    private val bindings: ReactiveList<Triple<EditorResult<String>, TypeInference, ReactiveSet<String>>>,
    private val bodyType: TypeInference,
    holder: ConstraintsHolder
) : AbstractTypeInference(context, holder) {
    private val observers = mutableListOf<Observer>()
    private val boundVars = bindings.asSet().map { b -> b.first.map { name -> name.orNull() } }.values()
    private val o1 = bindings.observe { recompute() }
    private val o2 = bindings.observeEach { (name, _, fvs) ->
        fvs.observeSet { ch -> if (ch.element in boundVars.now) recompute() } and name.observe { _ -> recompute() }
    }
    private val usedVariables = mutableSetOf<String>()

    init {
        addBindings()
    }

    private fun clear() {
        for (it in observers) it.kill()
        observers.clear()
        holder.clearConstraints()
        clearErrors()
        for (b in bindings.now) {
            val e = env(b)
            e.clear()
        }
        env(bodyType).clear()
        for (n in usedVariables) context.namer.release(n)
        usedVariables.clear()
    }

    private fun env(b: Triple<EditorResult<String>, TypeInference, ReactiveSet<String>>) = env(b.second)

    private fun env(inf: TypeInference) = inf.context.env as SimpleTIEnv

    private fun recompute() {
        clear()
        addBindings()
    }

    private fun addBindings() {
        val vertices = bindings.now
        val topo = computeSCCs(vertices)
        val env = mutableMapOf<String, ReactiveValue<CompileResult<TypeScheme>>>()
        val typeVars = List(vertices.size) { freshTypeVar() }
        computePrincipalTypes(topo, vertices, env, typeVars)
        addBackReferences(topo, vertices, typeVars)
        addEnvToBody(env)
    }

    private fun computePrincipalTypes(
        topo: List<List<Int>>,
        vertices: List<Triple<EditorResult<String>, TypeInference, ReactiveSet<String>>>,
        env: MutableMap<String, ReactiveValue<CompileResult<TypeScheme>>>,
        typeVars: List<Var>
    ) {
        for (comp in topo) {
            for (i in comp) {
                val e = env(vertices[i])
                for ((name, type) in env) {
                    val o = type.forEach { t -> t.ifOk { e.bind(name, it) } }
                    observers.add(o)
                }
                for (j in comp) {
                    val name = vertices[j].first.now.orNull() ?: continue
                    e.bind(name, TypeScheme(emptyList(), typeVars[j]))
                }
            }
            for (i in comp) {
                val n = vertices[i].first.now.orNull() ?: continue
                val inf = vertices[i].second
                val defTypeVar = typeVars[i]
                holder.bind(reactiveValue(ok(defTypeVar)), inf.type, this)
                env[n] = inf.type.flatMap { t ->
                    if (t is Ok) context.unificator.substitute(t.value)
                        .flatMap { context.env.generalize(it) }
                        .map { ok(it) }
                    else reactiveValue(childErr<TypeScheme>()).asBinding()
                }
            }
        }
    }

    private fun addBackReferences(
        topo: List<List<Int>>,
        vertices: List<Triple<EditorResult<String>, TypeInference, ReactiveSet<String>>>,
        typeVars: List<Var>
    ) {
        for (i in topo.indices) {
            for (j in 0 until i) {
                for (u in topo[i]) {
                    val name = vertices[u].first.now.orNull() ?: continue
                    val type = typeVars[u]
                    for (v in topo[j]) {
                        val e = env(vertices[v])
                        e.bind(name, TypeScheme(emptyList(), type))
                    }
                }
            }
        }
    }

    private fun addEnvToBody(env: Map<String, ReactiveValue<CompileResult<TypeScheme>>>) {
        for ((name, type) in env) {
            val e = bodyType.context.env as SimpleTIEnv
            val o = type.forEach { t ->
                if (t is Ok) e.bind(name, t.value)
                else e.unbind(name)
            }
            observers.add(o)
        }
    }

    private fun freshTypeVar(): Var {
        val n = context.namer.freshName()
        usedVariables.add(n)
        return Var(n)
    }

    private fun computeSCCs(vertices: List<Triple<EditorResult<String>, TypeInference, ReactiveSet<String>>>): List<List<Int>> {
        val index = mutableMapOf<String, Int>()
        for ((idx, b) in vertices.withIndex()) {
            b.first.now.ifOk { name -> index[name] = idx }
        }
        val adj = Array(vertices.size) { mutableListOf<Int>() }
        for ((i, b) in vertices.withIndex()) {
            for (v in b.third.now) {
                val j = index[v] ?: continue
                adj[j].add(i)
            }
        }
        val scc = SCCs(adj)
        scc.compute()
        return scc.topologicalSort()
    }

    override fun dispose() {
        super.dispose()
        clear()
        o1.kill()
        o2.kill()
        bodyType.dispose()
        bindings.now.forEach { it.second.dispose() }
    }

    override val type get() = bodyType.type
}