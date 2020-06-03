/**
 *@author Nikolaus Knop
 */

package hask.hextant.main

import bundles.createBundle
import hask.core.rt.eval
import hask.hextant.context.HaskInternal
import hask.hextant.editor.ExprExpander
import hask.hextant.ti.env.TIContext
import hextant.*
import hextant.command.line.*
import hextant.fx.registerShortcuts
import hextant.main.HextantApplication
import hextant.serial.makeRoot
import javafx.scene.Parent
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType.INFORMATION
import javafx.scene.layout.VBox
import reaktive.value.now
import validated.ifInvalid

class HaskEditorApplication : HextantApplication() {
    override fun createContext(root: Context): Context = HextantPlatform.defaultContext(root).apply {
        set(HaskInternal, TIContext, TIContext.root())
    }

    override fun createView(context: Context): Parent {
        val ti = context[HaskInternal, TIContext]
        val unificator = ti.unificator
        val editor = ExprExpander(context)
        editor.makeRoot()
        editor.inference.activate()
        val clContext = context.extend { set(SelectionDistributor, SelectionDistributor.newInstance()) }
        val cl = CommandLine(clContext, ContextCommandSource(context))
        val cli = CommandLineControl(cl, createBundle())
        val editorView = context.createView(editor)
        val box = VBox(editorView, cli)
        box.registerShortcuts {
            on("Ctrl+X") {
                val expr = editor.result.now.ifInvalid { return@on }
                val result = expr.eval().force()
                Alert(INFORMATION, result.toString()).show()
            }
            on("Ctrl+D") {
                println("Constraints:")
                for (c in unificator.constraints()) println(c)
                println("Unifier:")
                for (s in unificator.substitutions()) println("${s.key} = ${s.value}")
                println("${ti.namer}")
            }
            on("Ctrl+P") {
                cli.receiveFocus()
            }
            on("INSERT") {
                val selected = context[SelectionDistributor].selectedView
                selected.now?.focus()
            }
        }
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

