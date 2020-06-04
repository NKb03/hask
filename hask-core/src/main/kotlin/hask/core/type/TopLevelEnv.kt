/**
 *@author Nikolaus Knop
 */

package hask.core.type

import hask.core.ast.ADTDef
import hask.core.ast.Expr.*
import hask.core.rt.*
import hask.core.rt.NormalForm.ADTValue
import hask.core.type.Type.ParameterizedADT
import hask.core.type.Type.Var

class TopLevelEnv(private val adtDefinitions: Collection<ADTDef>) {
    private val constructorToADTName = adtDefinitions.flatMap { (adt, constructors) ->
        constructors.map { it.name to adt }
    }.toMap()

    private val parameters = adtDefinitions.flatMap { it.constructors }.associate { it.name to it.parameters }

    fun getADT(constructor: String) =
        constructorToADTName[constructor] ?: error("Unresolved ADT constructor '$constructor'")

    fun getParameters(constructor: String) = parameters[constructor]

    val env = adtDefinitions.flatMap { (adt, constructors) ->
        constructors.map { c ->
            val returnType = ParameterizedADT(adt.name, adt.typeParameters.map { Var(it) })
            val scheme = TypeScheme(adt.typeParameters, functionType(c.parameters, returnType))
            c.name to scheme
        }
    }

    fun putConstructorFunctions(frame: StackFrame) {
        for ((adt, constructors) in adtDefinitions) {
            for (c in constructors) {
                if (c.parameters.isEmpty()) {
                    frame.put(c.name, Thunk.strict(ADTValue(c, emptyList())))
                } else {
                    val parameterNames = c.parameters.indices.map { i -> "p$i" }
                    val parameterTypes = adt.typeParameters.map { Var(it) }
                    val returnType = ParameterizedADT(adt.name, parameterTypes)
                    val arguments = parameterNames.map { name -> ValueOf(name) }
                    val body = ApplyBuiltin(c.name, parameterTypes, returnType, arguments) { args -> ADTValue(c, args) }
                    val func = Lambda(parameterNames, body)
                    frame.put(c.name, func.eval(frame))
                }
            }
        }
    }
}