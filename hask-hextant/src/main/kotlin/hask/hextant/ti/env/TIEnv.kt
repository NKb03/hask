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

class TIEnv(
    private val parent: TIEnv?,
    private val namer: ReleasableNamer
) {
    constructor(namer: ReleasableNamer) : this(root(namer), namer)

    private val declaredBindings = mutableMapOf<String, CompileResult<TypeScheme>>()

    private val fvSet = reactiveSet<String>()

    private val myFVS = Counter(fvSet.now)

    val freeTypeVars: ReactiveSet<String> =
        if (parent == null) fvSet else fvSet + parent.freeTypeVars

    private val queries = mutableMapOf<String, MutableList<ReactiveVariable<CompileResult<Type>?>>>()

    private fun getQueries(name: String): List<ReactiveVariable<CompileResult<Type>?>> = queries[name] ?: emptyList()

    fun bind(name: String, type: CompileResult<TypeScheme>) {
        if (declaredBindings[name] == type) return
        declaredBindings[name] = type
        type.ifOk { myFVS.addAll(it.fvs()) }
        getQueries(name).forEach { it.set(type.map { it.instantiate(namer) }) }
    }

    private fun bind(name: String, type: TypeScheme) {
        bind(name, ok(type))
    }

    private fun bind(name: String, type: Type) {
        bind(name, TypeScheme(emptyList(), type))
    }

    fun unbind(name: String) {
        if (name !in declaredBindings) return
        val type = declaredBindings.remove(name)!!
        type.ifOk { myFVS.removeAll(it.fvs()) }
        getQueries(name).forEach { it.set(null) }
    }

    fun resolve(name: String): ReactiveValue<CompileResult<Type>?> {
        val queries = queries.getOrPut(name) { mutableListOf() }
        val tpe = declaredBindings[name]?.map { it.instantiate(namer) }
        val referent = reactiveVariable(tpe)
        queries.add(referent)
        return if (parent != null)
            referent.orElse(parent.resolve(name))
        else referent
    }

    val now: Map<String, CompileResult<TypeScheme>>
        get() {
            val res = mutableMapOf<String, CompileResult<TypeScheme>>()
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

    companion object {
        private fun root(namer: ReleasableNamer): TIEnv = TIEnv(null, namer).apply {
            bind("add", Func(INT, Func(INT, INT)))
            bind("sub", Func(INT, Func(INT, INT)))
            bind("mul", Func(INT, Func(INT, INT)))
            bind("div", Func(INT, Func(INT, INT)))
            bind("eq", TypeScheme(listOf("a"), Func(Var("a"), Func(Var("a"), BoolT))))
            bind("True", BoolT)
            bind("False", BoolT)
        }
    }
}