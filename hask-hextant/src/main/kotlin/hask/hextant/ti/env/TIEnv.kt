/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti.env

import hask.core.ast.Builtin.Companion.BooleanT
import hask.core.type.Type
import hask.core.type.Type.*
import hask.core.type.TypeScheme
import kollektion.Counter
import reaktive.asValue
import reaktive.set.*
import reaktive.value.*
import reaktive.value.binding.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

class TIEnv(private val parent: TIEnv?, private val namer: ReleasableNamer) {
    constructor(namer: ReleasableNamer) : this(root(namer), namer)

    private val declaredBindings = mutableMapOf<String, TypeScheme>()

    private val fvSet = reactiveSet<String>()

    private val myFVS = Counter(fvSet.now)

    val freeTypeVars: ReactiveSet<String> =
        if (parent == null) fvSet else fvSet + parent.freeTypeVars

    private val queries = mutableMapOf<String, MutableList<ReactiveVariable<Type?>>>()
    private val isResolvedQueries = mutableMapOf<String, ReactiveVariable<Boolean>>()

    private fun getQueries(name: String): List<ReactiveVariable<Type?>> = queries[name] ?: emptyList()

    fun bind(name: String, type: TypeScheme) {
        if (declaredBindings[name] == type) return
        declaredBindings[name] = type
        myFVS.addAll(type.fvs())
        getQueries(name).forEach { it.set(type.instantiate(namer)) }
        isResolvedQueries[name]?.set(true)
    }

    fun bind(name: String, type: Type) {
        bind(name, TypeScheme(emptyList(), type))
    }

    fun unbind(name: String) {
        if (name !in declaredBindings) return
        val type = declaredBindings.remove(name)!!
        myFVS.removeAll(type.fvs())
        getQueries(name).forEach { it.set(null) }
        isResolvedQueries[name]?.set(false)
    }

    fun declaredType(name: String): TypeScheme? = declaredBindings[name]

    fun resolve(name: String): ReactiveValue<Type?> {
        val queries = queries.getOrPut(name) { mutableListOf() }
        val tpe = declaredBindings[name]?.instantiate(namer)
        val referent = reactiveVariable(tpe)
        queries.add(referent)
        return if (parent != null)
            referent.orElse(parent.resolve(name))
        else referent
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

    fun child() = TIEnv(this, namer)

    fun generalize(t: Type): ReactiveValue<TypeScheme> {
        val fvs = unmodifiableReactiveSet(t.fvs())
        val typeParameters = fvs - this.freeTypeVars
        return typeParameters.asValue().map { TypeScheme(it.now.toList(), t) }
    }

    fun clear() {
        declaredBindings.clear()
        myFVS.clear()
        queries.forEach { (_, q) ->
            q.forEach { it.set(null) }
        }
    }

    fun isResolved(name: String): ReactiveValue<Boolean> {
        val q = isResolvedQueries.getOrPut(name) { reactiveVariable(name in declaredBindings) }
        return if (parent != null) q.or(parent.isResolved(name)) else q
    }

    companion object {
        private fun root(namer: ReleasableNamer): TIEnv = TIEnv(null, namer).apply {
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