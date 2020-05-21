/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti

import com.natpryce.hamkrest.*
import com.natpryce.hamkrest.should.shouldMatch
import hask.core.type.Type.INT
import hask.hextant.ti.env.TIContext
import hextant.ok
import org.junit.jupiter.api.Test
import reaktive.list.reactiveList
import reaktive.set.emptyReactiveSet
import reaktive.value.reactiveValue
import reaktive.value.reactiveVariable

class TypeInferenceTests {
    @Test
    fun `simple application type inference`() { // (\x -> x) 1 : int
        val context = TIContext.root()
        val parameters = reactiveList(ok("x"))
        val ref = reactiveVariable(ok("x"))
        val lambdaContext = context.child()
        val body = ReferenceTypeInference(lambdaContext.child(), ref)
        body.activate()
        val lambda = LambdaTypeInference(lambdaContext, parameters, body)
        val application = ApplyTypeInference(
            lambdaContext.child(),
            lambda,
            reactiveList(IntLiteralTypeInference(lambdaContext.child()))
        )
        application.assertType(INT)
    }

    @Test
    fun `apply integer literal should fail`() { // 2 1
        val context = TIContext.root()
        val left = IntLiteralTypeInference(context.child())
        val right = IntLiteralTypeInference(context.child())
        val app = ApplyTypeInference(context, left, reactiveList(right))
        app.errors.now shouldMatch hasSize(equalTo(1))
    }

    @Test
    fun `occurs check should fail`() { //\x -> x x
        val context = TIContext.root()
        val parameters = reactiveList(ok("x"))
        val ref = reactiveVariable(ok("x"))
        val bodyContext = context.child()
        val body = ApplyTypeInference(
            bodyContext,
            ReferenceTypeInference(bodyContext.child(), ref).also { it.activate() },
            reactiveList(ReferenceTypeInference(bodyContext.child(), ref).also { it.activate() })
        )
        val lambda = LambdaTypeInference(context.child(), parameters, body)
    }

    //let id = ;root
    //  \x -> ;letValue1
    //     x ;lambdaBody
    //in
    //  let x = ;letBody1
    //    id ;letValue2
    //  in 1 ;letBody2
    @Test
    fun `apply id in value of let`() {
        val rootCtx = TIContext.root()
        val letValue1Ctx = rootCtx.child()
        val letBody1Ctx = rootCtx.child()
        val lambdaBodyCtx = letValue1Ctx.child()
        val letValue2Ctx = letBody1Ctx.child()
        val letBody2Ctx = letBody1Ctx.child()
        val lambdaBody = ReferenceTypeInference(lambdaBodyCtx, reactiveValue(ok("x")))
        lambdaBody.activate()
        val letValue1 = LambdaTypeInference(letValue1Ctx, reactiveList(ok("x")), lambdaBody)
        letValue1.activate()
        val letValue2 = ReferenceTypeInference(letValue2Ctx, reactiveValue(ok("x")))
        letValue2.activate()
        val letBody2 = IntLiteralTypeInference(letBody2Ctx)
        letBody2.activate()
        val letBody1 = LetTypeInference(
            letBody1Ctx,
            { listOf(Pair(ok("x"), letValue2)) },
            DependencyGraph(reactiveList(Pair(reactiveValue(ok("x")), emptyReactiveSet()))),
            letBody2
        )
        letBody1.activate()
        val root = LetTypeInference(
            rootCtx,
            { listOf(Pair(ok("id"), letValue1)) },
            DependencyGraph(reactiveList(Pair(reactiveValue(ok("id")), emptyReactiveSet()))),
            letBody1
        )
        root.activate()
        root.errors.now shouldMatch isEmpty
        root.assertType(INT)
    }
}