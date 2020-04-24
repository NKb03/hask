/**
 * @author Nikolaus Knop
 */

package hask.core.type

typealias Env = Map<String, TypeScheme>

val noEnv: Env = emptyMap()

fun Env.fvs() = values.flatMap { it.fvs() }

data class Constraint(val l: Type, val r: Type) {
    override fun toString(): String = "$l = $r"

    fun apply(subst: Subst) = Constraint(l.apply(subst), r.apply(subst))
}

typealias Constraints = List<Constraint>

fun Constraints.apply(subst: Subst) = map { it.apply(subst) }

fun MutableList<Constraint>.bind(type: Type, to: Type) = add(
    Constraint(
        type,
        to
    )
)

typealias Subst = Map<String, Type>

fun Subst.apply(subst: Subst) = mapValues { (_, t) -> t.apply(subst) }

infix fun Subst.compose(other: Subst) = this + other.apply(this)
