/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti.env

import hask.core.type.*
import hask.core.type.Type.*
import hask.hextant.ti.Builtins.BoolT
import hask.hextant.ti.impl.Counter
import hextant.*
import reaktive.asValue
import reaktive.set.*
import reaktive.value.*
import reaktive.value.binding.map
import reaktive.value.binding.orElse

class SimpleTIEnv(
    private val parent: TIEnv?,
    private val namer: ReleasableNamer
) : TIEnv {
    constructor(namer: ReleasableNamer) : this(RootEnv(namer), namer)

    private val declaredBindings = mutableMapOf<String, CompileResult<TypeScheme>>()

    private val fvSet = reactiveSet<String>()

    private val myFVS = Counter(fvSet.now)

    override val freeTypeVars: ReactiveSet<String> =
        if (parent == null) fvSet else fvSet + parent.freeTypeVars

    private val queries = mutableMapOf<String, MutableList<ReactiveVariable<CompileResult<Type>?>>>()

    private fun getQueries(name: String): List<ReactiveVariable<CompileResult<Type>?>> = queries[name] ?: emptyList()

    fun bind(name: String, type: CompileResult<TypeScheme>) {
        if (declaredBindings[name] == type) return
        declaredBindings[name] = type
        type.ifOk { myFVS.addAll(it.fvs()) }
        getQueries(name).forEach { it.set(type.map { it.instantiate(namer) }) }
    }

    fun unbind(name: String) {
        if (name !in declaredBindings) return
        val type = declaredBindings.remove(name)!!
        type.ifOk { myFVS.removeAll(it.fvs()) }
        getQueries(name).forEach { it.set(null) }
    }

    override fun resolve(name: String): ReactiveValue<CompileResult<Type>?> {
        val queries = queries.getOrPut(name) { mutableListOf() }
        val tpe = declaredBindings[name]?.map { it.instantiate(namer) }
        val referent = reactiveVariable(tpe)
        queries.add(referent)
        return if (parent != null)
            referent.orElse(parent.resolve(name))
        else referent
    }

    override val now: Map<String, CompileResult<TypeScheme>>
        get() {
            val res = mutableMapOf<String, CompileResult<TypeScheme>>()
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
        private fun entry(name: String, type: Type) = entry(name, TypeScheme(emptyList(), type))
        private fun entry(name: String, scheme: TypeScheme) = name to ok(scheme)

        override val now: Map<String, CompileResult<TypeScheme>> = mapOf(
            entry("add", Func(INT, Func(INT, INT))),
            entry("sub", Func(INT, Func(INT, INT))),
            entry("mul", Func(INT, Func(INT, INT))),
            entry("div", Func(INT, Func(INT, INT))),
            entry("eq", TypeScheme(listOf("a"), Func(Var("a"), Func(Var("a"), BoolT)))),
            entry("True", BoolT),
            entry("False", BoolT)
        )

        override fun resolve(name: String) = reactiveValue(now[name]?.map { it.instantiate(namer) })

        override fun generalize(t: Type): ReactiveValue<TypeScheme> = reactiveValue(t.generalize(now.keys))

        override val freeTypeVars: ReactiveSet<String> = emptyReactiveSet()
    }
}