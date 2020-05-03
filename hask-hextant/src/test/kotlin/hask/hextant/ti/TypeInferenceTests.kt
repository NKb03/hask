/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti

import com.natpryce.hamkrest.*
import com.natpryce.hamkrest.should.shouldMatch
import hask.core.type.Type.INT
import hask.hextant.ti.env.TIContext
import hask.hextant.ti.unify.ConstraintsHolderFactory
import hextant.ok
import org.junit.jupiter.api.Test
import reaktive.list.reactiveList
import reaktive.set.reactiveSet
import reaktive.value.reactiveValue
import reaktive.value.reactiveVariable

class TypeInferenceTests {
    @Test
    fun `simple application type inference`() { // (\x -> x) 1 : int
        val context = TIContext.root()
        val factory = ConstraintsHolderFactory.unifying(context.unificator)
        val parameterName = reactiveVariable(ok("x"))
        val ref = reactiveVariable(ok("x"))
        val lambdaContext = context.child()
        val body = ReferenceTypeInference(lambdaContext.child(), ref, factory.createHolder())
        val lambda = LambdaTypeInference(lambdaContext, parameterName, body, factory.createHolder())
        val application = ApplyTypeInference(
            context,
            lambda,
            IntLiteralTypeInference(context.child(), factory.createHolder()),
            factory.createHolder()
        )
        application.assertType(INT)
    }

    @Test
    fun `apply integer literal should fail`() { // 2 1
        val context = TIContext.root()
        val factory = ConstraintsHolderFactory.unifying(context.unificator)
        val left = IntLiteralTypeInference(context.child(), factory.createHolder())
        val right = IntLiteralTypeInference(context.child(), factory.createHolder())
        val app = ApplyTypeInference(context, left, right, factory.createHolder())
        app.errors.now shouldMatch hasSize(equalTo(1))
    }

    @Test
    fun `occurs check should fail`() { //\x -> x x
        val context = TIContext.root()
        val factory = ConstraintsHolderFactory.unifying(context.unificator)
        val parameterName = reactiveVariable(ok("x"))
        val ref = reactiveVariable(ok("x"))
        val bodyContext = context.child()
        val body = ApplyTypeInference(
            bodyContext,
            ReferenceTypeInference(bodyContext.child(), ref, factory.createHolder()),
            ReferenceTypeInference(bodyContext.child(), ref, factory.createHolder()),
            factory.createHolder()
        )
        val lambda = LambdaTypeInference(context, parameterName, body, factory.createHolder())
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
        val factory = ConstraintsHolderFactory.unifying(rootCtx.unificator)
        val letValue1Ctx = rootCtx.child()
        val letBody1Ctx = rootCtx.child()
        val lambdaBodyCtx = letValue1Ctx.child()
        val letValue2Ctx = letBody1Ctx.child()
        val letBody2Ctx = letBody1Ctx.child()
        val lambdaBody = ReferenceTypeInference(lambdaBodyCtx, reactiveValue(ok("x")), factory.createHolder())
        val letValue1 = LambdaTypeInference(letValue1Ctx, reactiveValue(ok("x")), lambdaBody, factory.createHolder())
        val letValue2 = ReferenceTypeInference(letValue2Ctx, reactiveValue(ok("x")), factory.createHolder())
        val letBody2 = IntLiteralTypeInference(letBody2Ctx, factory.createHolder())
        val letBody1 = LetTypeInference(
            letBody1Ctx,
            reactiveList(Triple(reactiveValue(ok("x")), letValue2, reactiveSet())),
            letBody2,
            factory.createHolder()
        )
        val root = LetTypeInference(
            rootCtx,
            reactiveList(Triple(reactiveValue(ok("id")), letValue1, reactiveSet())),
            letBody1,
            factory.createHolder()
        )
        root.errors.now shouldMatch isEmpty
        root.assertType(INT)
    }
}