/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import com.natpryce.hamkrest.should.shouldMatch
import hask.core.type.Type.Func
import hask.core.type.Type.INT
import hask.hextant.context.HaskInternal
import hask.hextant.ti.Builtins.BoolT
import hask.hextant.ti.assertType
import hask.hextant.ti.env.TIContext
import hask.hextant.ti.unify.ConstraintsHolderFactory
import hextant.test.*
import org.junit.jupiter.api.Test
import reaktive.value.now

class EditorFlowTests {
    /*
    * let id<0> = \x -> x in (id<1> id<2>) 1
    * id<0>                                     :: forall a . a -> a
    * id<1>                                     :: b -> b
    * id<2>                                     :: c -> c
    * 1                                         :: int
    * id<1> id<2>                               :: d
    * (id<1> id<2>) 1                           :: e
    * let id<0> = \x -> x in (id<1> id<2>) 1    :: f
    * Constraints:
    * b -> b = (c -> c) -> d
    *
    *
    */
    @Test
    fun `use id function for different types`() {
        val ti = TIContext.root()
        val context = testingContext {
            set(HaskInternal, TIContext, ti)
            set(
                HaskInternal,
                ConstraintsHolderFactory, ConstraintsHolderFactory.unifying(ti.unificator)
            )
        }
        val root = ExprExpander(context)
        root.expand<LetEditor>("let") {
            bindings.addLast()!!.apply {
                name.setText("id")
                with(value.doExpand<LambdaEditor>("lambda")) {
                    parameters.addLast()!!.setText("x")
                    body.doExpandTo("x")
                }
            }
            with(body.doExpand<ApplyEditor>("apply")) {
                with(applied.doExpand<ApplyEditor>("apply")) {
                    applied.doExpandTo("id")
                    argument.doExpandTo("id")
                }
                argument.doExpandTo("1")
            }
        }
        root.inference.errors.now shouldEqual emptySet()
        root.inference.assertType(INT)
    }

    /*
     * let id = \x -> x in let x = id 1 in 1
    */
    @Test
    fun `use id in value of let binding`() {
        val ti = TIContext.root()
        val context = testingContext {
            set(HaskInternal, TIContext, ti)
            set(
                HaskInternal,
                ConstraintsHolderFactory, ConstraintsHolderFactory.unifying(ti.unificator)
            )
        }
        val root = ExprExpander(context)
        root.expand<LetEditor>("let") {
            with(bindings.addLast()!!) {
                name.setText("id")
                with(value.doExpand<LambdaEditor>("lambda")) {
                    parameters.addLast()!!.setText("x")
                    body.doExpandTo("x")
                }
                body.expand<LetEditor>("let") {
                    with(bindings.addLast()!!) {
                        name.setText("x")
                        value.doExpandTo("id")
                        value.wrapInApply()
                        with(value.editor.now as ApplyEditor) {
                            argument.doExpandTo("1")
                        }
                    }
                    body.doExpandTo("1")
                }
            }
        }
        root.inference.errors.now shouldMatch isEmpty
        root.inference.assertType(INT)
    }

    /*
     * eq 1
    */
    @Test
    fun `eq 1 should not be an error`() {
        val ti = TIContext.root()
        val context = testingContext {
            set(HaskInternal, TIContext, ti)
            set(
                HaskInternal,
                ConstraintsHolderFactory, ConstraintsHolderFactory.unifying(ti.unificator)
            )
        }
        val root = ExprExpander(context)
        root.expand<ApplyEditor>("apply") {
            applied.doExpandTo("eq")
            argument.doExpandTo("1")
            inference.errors.now shouldEqual emptySet()
        }
        root.inference.assertType(Func(INT, BoolT))
    }
}