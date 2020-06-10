package hask.hextant.ti

import hask.core.ast.Pattern
import hask.core.ast.Pattern.*
import hask.core.ast.Pattern.Variable
import hask.core.type.Type
import hask.core.type.Type.*
import hask.hextant.ti.env.*
import hask.hextant.ti.unify.Constraint
import reaktive.collection.forEach
import reaktive.collection.observeCollection
import reaktive.dependencies
import reaktive.list.ReactiveList
import reaktive.list.binding.values
import reaktive.value.*
import reaktive.value.binding.binding
import reaktive.value.binding.flatMap
import validated.*

class MatchTypeInference(
    context: TIContext,
    private val matched: TypeInference,
    private val cases: ReactiveList<Pair<ReactiveValue<Validated<Pattern>>, ExpanderTypeInference>>,
    private val adtDefinitions: ADTDefinitions
) : AbstractTypeInference(context) {
    private val returnType by typeVariable()

    private val _type = reactiveVariable<Type>(Hole)

    override fun onActivate() {
        _type.set(returnType)
        val constraints = cases.map { (pattern, body) ->
            pattern.flatMap { p ->
                val infos = p.map {
                    it.usedConstructors().map { c ->
                        adtDefinitions.getInfo(c)
                    }
                }.ifInvalid { emptyList() }
                val usedNames = mutableSetOf<String>()
                binding<Constraint?>(dependencies(infos + type)) {
                    body.context.env.clear()
                    usedNames.forEach { name -> releaseName(name) }
                    usedNames.clear()
                    p.map {
                        Constraint(
                            it.inferExpectedType(matched.type.now, body.context.env, usedNames),
                            matched.type.now,
                            this
                        )
                    }.orNull()
                }
            }
        }.values()
        addObserver(constraints.observeCollection(
            added = { _, c -> if (c != null) addConstraint(c) },
            removed = { _, c -> if (c != null) removeConstraint(c) }
        ))
        val armTypes = cases.map { (_, v) -> v.type }.values()
        armTypes.forEach { t -> addConstraint(returnType, t) }
        addObserver(
            armTypes.observeCollection(
                added = { _, t -> addConstraint(returnType, t) },
                removed = { _, t -> removeConstraint(returnType, t) }
            ))
    }

    private fun Pattern.usedConstructors(dest: MutableSet<String> = mutableSetOf()): Set<String> = when (this) {
        is Destructuring -> {
            dest.add(constructor)
            components.forEach { it.usedConstructors(dest) }
            dest
        }
        else             -> dest
    }

    private fun Pattern.inferExpectedType(subject: Type, env: TIEnv, usedNames: MutableSet<String>): Type =
        when (this) {
            Wildcard         -> Var(freshName().also { usedNames.add(it) })
            is Variable      -> {
                env.bind(name, subject)
                Var(freshName().also { usedNames.add(it) })
            }
            is Integer       -> INT
            is Destructuring -> run {
                val (adt, cstr) = adtDefinitions.getInfo(constructor).now ?: return Hole
                val instantiation = adt.typeParameters.associateWith { Var(freshName().also { usedNames.add(it) }) }
                val parameters = cstr.parameters.map { it.apply(instantiation) }
                parameters.zip(components).forEach { (parameter, pattern) ->
                    pattern.inferExpectedType(parameter, env, usedNames)
                }
                ParameterizedADT(adt.name, adt.typeParameters.map { instantiation.getValue(it) })
            }
        }


    override val type: ReactiveValue<Type> get() = _type

    override fun children(): Collection<TypeInference> = cases.now.map { (_, ti) -> ti } + matched
}
