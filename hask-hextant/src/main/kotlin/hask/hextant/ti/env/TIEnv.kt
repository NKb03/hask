/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti.env

import hask.core.ast.Builtin.Companion.BooleanT
import hask.core.type.*
import hask.core.type.Type.*
import kollektion.Counter
import reaktive.map
import reaktive.set.*
import reaktive.set.binding.SetBinding
import reaktive.value.*
import reaktive.value.binding.or
import reaktive.value.binding.orElse
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

class TIEnv(private val parent: TIEnv?) {
    constructor() : this(root())

    private val declaredBindings = mutableMapOf<String, TypeScheme>()

    private val fvSet = reactiveSet<String>()

    private val myFVS = Counter(fvSet.now)

    val freeTypeVars: ReactiveSet<String> =
        if (parent == null) fvSet else fvSet + parent.freeTypeVars

    private val queries = mutableMapOf<String, MutableList<Pair<Namer, ReactiveVariable<Type?>>>>()
    private val isResolvedQueries = mutableMapOf<String, ReactiveVariable<Boolean>>()

    private fun getQueries(name: String): List<Pair<Namer, ReactiveVariable<Type?>>> = queries[name] ?: emptyList()

    fun bind(name: String, type: TypeScheme) {
        if (declaredBindings[name] == type) return
        declaredBindings[name] = type
        myFVS.addAll(type.fvs())
        getQueries(name).forEach { (namer, t) -> t.set(type.instantiate(namer)) }
        isResolvedQueries[name]?.set(true)
    }

    fun bind(name: String, type: Type) {
        bind(name, TypeScheme(emptyList(), type))
    }

    fun unbind(name: String) {
        if (name !in declaredBindings) return
        val type = declaredBindings.remove(name)!!
        myFVS.removeAll(type.fvs())
        getQueries(name).forEach { (_, t) -> t.set(null) }
        isResolvedQueries[name]?.set(false)
    }

    fun declaredType(name: String): TypeScheme? = declaredBindings[name]

    fun resolve(name: String, namer: Namer): ReactiveValue<Type?> {
        val queries = queries.getOrPut(name) { mutableListOf() }
        val tpe = declaredBindings[name]?.instantiate(namer)
        val t = reactiveVariable(tpe)
        val referent = Pair(namer, t)
        queries.add(referent)
        return if (parent != null) t.orElse(parent.resolve(name, namer))
        else t
    }

    val now: Map<String, TypeScheme>
        get() {
            val res = mutableMapOf<String, TypeScheme>()
            var env: TIEnv = this
            while (true) {
                res.putAll(env.declaredBindings)
                env = env.parent ?: break
            }
            return res
        }

    fun child() = TIEnv(this)

    fun generalize(t: Type): ReactiveValue<TypeScheme> {
        val fvs = unmodifiableReactiveSet(t.fvs())
        val typeParameters = fvs - this.freeTypeVars
        return typeParameters.map { names: SetBinding<String> -> TypeScheme(names.now.toList(), t) }
    }

    fun clear() {
        declaredBindings.clear()
        myFVS.clear()
        queries.forEach { (_, q) ->
            q.forEach { (_, t) -> t.set(null) }
        }
    }

    fun isResolved(name: String): ReactiveValue<Boolean> {
        val q = isResolvedQueries.getOrPut(name) { reactiveVariable(name in declaredBindings) }
        return if (parent != null) q.or(parent.isResolved(name)) else q
    }

    companion object {
        private fun root(): TIEnv = TIEnv(null).apply {
            bind("add", Func(INT, Func(INT, INT)))
            bind("sub", Func(INT, Func(INT, INT)))
            bind("mul", Func(INT, Func(INT, INT)))
            bind("div", Func(INT, Func(INT, INT)))
            bind("eq", TypeScheme(listOf("a"), Func(Var("a"), Func(Var("a"), BooleanT))))
            bind("True", BooleanT)
            bind("False", BooleanT)
        }
    }
}