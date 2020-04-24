/**
 *@author Nikolaus Knop
 */

package hask.hextant.main

import hask.core.rt.eval
import hask.hextant.context.HaskInternal
import hask.hextant.editor.ExprExpander
import hask.hextant.ti.unify.ConstraintsHolderFactory
import hask.hextant.ti.env.TIContext
import hextant.*
import hextant.bundle.createBundle
import hextant.command.line.*
import hextant.fx.registerShortcuts
import hextant.main.HextantApplication
import javafx.scene.Parent
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType.INFORMATION
import javafx.scene.layout.VBox
import reaktive.value.now

class HaskEditorApplication : HextantApplication() {
    override fun createContext(root: Context): Context = HextantPlatform.defaultContext(root).apply {
        set(HaskInternal,
            TIContext, TIContext.root())
    }

    override fun createView(context: Context): Parent {
        val unifier = context[HaskInternal, TIContext].unificator
        context[HaskInternal, ConstraintsHolderFactory] = ConstraintsHolderFactory.unifying(unifier)
        val editor = ExprExpander(context)
        val cl = CommandLine(context, ContextCommandSource(context))
        val cli = CommandLineControl(cl, createBundle())
        val editorView = context.createView(editor).apply {
            registerShortcuts {
                on("Ctrl+E") {
                    val expr = editor.result.now.ifErr { return@on }
                    val result = expr.eval().force()
                    Alert(INFORMATION, result.toString()).show()
                }
                on("Ctrl+D") {
                    println(unifier.substitutions())
                }
            }

        }
        return VBox(editorView, cli)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(HaskEditorApplication::class.java)
        }
    }
}

