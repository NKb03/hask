package hask.core.type

import hask.core.type.Type.Var

data class TypeScheme(val names: Set<String>, val body: Type) {
    fun fvs(): Set<String> = body.fvs() - names

    fun apply(subst: Subst): Type = body.apply(subst - names)

    override fun toString(): String = buildString {
        if (names.isNotEmpty()) {
            append("forall ")
            names.joinTo(this, " ")
            append(" . ")
        }
        append(body)
    }

    fun instantiate(namer: Namer): Type {
        val subst = names.associateWith { Var(namer.freshName()) }
        return body.apply(subst)
    }
}
