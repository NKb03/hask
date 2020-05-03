/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti.env

import hask.core.type.*
import hask.hextant.ti.Builtins.BoolT
import hask.core.type.Type.*
import hask.hextant.ti.impl.Counter
import reaktive.asValue
import reaktive.set.*
import reaktive.value.*
import reaktive.value.binding.map
import reaktive.value.binding.orElse

class SimpleTIEnv(
    private val parent: TIEnv?,
    private val namer: ReleasableNamer
) : TIEnv {
    constructor(namer: ReleasableNamer) : this(
        RootEnv(
            namer
        ), namer)

    private val declaredBindings = mutableMapOf<String, TypeScheme>()

    private val fvSet = reactiveSet<String>()

    private val myFVS = Counter(fvSet.now)

    override val freeTypeVars: ReactiveSet<String> =
        if (parent == null) fvSet else fvSet + parent.freeTypeVars

    private val queries = mutableMapOf<String, MutableList<ReactiveVariable<Type?>>>()

    private fun getQueries(name: String): List<ReactiveVariable<Type?>> = queries[name] ?: emptyList()

    fun bind(name: String, type: TypeScheme) {
        if (declaredBindings[name] == type) return
        declaredBindings[name] = type
        myFVS.addAll(type.fvs())
        getQueries(name).forEach { it.set(type.instantiate(namer)) }
    }

    fun unbind(name: String) {
        if (name !in declaredBindings) return
        val type = declaredBindings.remove(name)!!
        myFVS.removeAll(type.fvs())
        getQueries(name).forEach { it.set(null) }
    }

    override fun resolve(name: String): ReactiveValue<Type?> {
        val queries = queries.getOrPut(name) { mutableListOf() }
        val tpe = declaredBindings[name]?.instantiate(namer)
        val referent = reactiveVariable(tpe)
        queries.add(referent)
        return if (parent != null)
            referent.orElse(parent.resolve(name))
        else referent
    }

    override val now: Map<String, TypeScheme>
        get() {
            val res = mutableMapOf<String, TypeScheme>()
            var env: TIEnv = this
            while (true) {
                if (env is SimpleTIEnv) {
                    res.putAll(env.declaredBindings)
                    env = env.parent ?: break
                } else {
                    res.putAll(env.now)
                    break
                }
            }
            return res
        }

    fun child() = SimpleTIEnv(this, namer)

    override fun generalize(t: Type): ReactiveValue<TypeScheme> {
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

    private class RootEnv(private val namer: Namer) : TIEnv {
        override val now: Map<String, TypeScheme> = mapOf(
            "add" to TypeScheme(emptyList(), Func(INT, Func(INT, INT))),
            "sub" to TypeScheme(emptyList(), Func(INT, Func(INT, INT))),
            "mul" to TypeScheme(emptyList(), Func(INT, Func(INT, INT))),
            "div" to TypeScheme(emptyList(), Func(INT, Func(INT, INT))),
            "eq" to TypeScheme(listOf("a"), Func(Var("a"), Func(Var("a"), BoolT))),
            "True" to TypeScheme(emptyList(), BoolT),
            "False" to TypeScheme(emptyList(), BoolT)
        )

        override fun resolve(name: String): ReactiveValue<Type?> = reactiveValue(now[name]?.instantiate(namer))

        override fun generalize(t: Type): ReactiveValue<TypeScheme> = reactiveValue(t.generalize(now))

        override val freeTypeVars: ReactiveSet<String> = emptyReactiveSet()
    }
}