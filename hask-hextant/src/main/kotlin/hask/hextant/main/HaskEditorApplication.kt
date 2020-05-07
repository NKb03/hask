/**
 *@author Nikolaus Knop
 */

package hask.hextant.main

import hask.core.rt.eval
import hask.hextant.context.HaskInternal
import hask.hextant.editor.ExprExpander
import hask.hextant.editor.inferences
import hask.hextant.ti.AbstractTypeInference
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
        set(HaskInternal, TIContext, TIContext.root())
    }

    override fun createView(context: Context): Parent {
        val ti = context[HaskInternal, TIContext]
        val unificator = ti.unificator
        val editor = ExprExpander(context)
        editor.inference.activate()
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
                    println("Constraints:")
                    for (c in unificator.constraints()) println(c)
                    println("Unifier:")
                    for (s in unificator.substitutions()) println("${s.key} = ${s.value}")
                    println("${ti.namer}")
                    val tree = editor.inferences()
                    val active = AbstractTypeInference.actives()
                    println("In tree $tree")
                    println("Active: $active")
                    println("In tree but not active: ${tree - active}")
                    println("Active but not in tree: ${active - tree}")
                }
                on("ESCAPE") {
                    cli.receiveFocus()
                }
            }

        }
        val box = VBox(editorView, cli)
        box.setPrefSize(500.0, 500.0)
        return box
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(HaskEditorApplication::class.java)
        }
    }
}

