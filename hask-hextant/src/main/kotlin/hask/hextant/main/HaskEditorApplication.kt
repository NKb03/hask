/**
 *@author Nikolaus Knop
 */

package hask.hextant.main

import bundles.createBundle
import hask.core.rt.eval
import hask.core.rt.evaluate
import hask.core.type.TopLevelEnv
import hask.hextant.context.HaskInternal
import hask.hextant.editor.ProgramEditor
import hask.hextant.ti.env.TIContext
import hextant.*
import hextant.command.line.*
import hextant.context.*
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
        val editor = ProgramEditor(context)
        editor.makeRoot()
        editor.expr.inference.activate()
        val clContext = context.extend { set(SelectionDistributor, SelectionDistributor.newInstance()) }
        val cl = CommandLine(clContext, ContextCommandSource(context))
        val cli = CommandLineControl(cl, createBundle())
        val editorView = context.createView(editor)
        val box = VBox(editorView, cli)
        box.registerShortcuts {
            on("Ctrl+X") {
                val program = editor.result.now.ifInvalid { return@on }
                val tl = TopLevelEnv(program.adtDefs)
                val result = program.expr.evaluate(tl)
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
            on("Ctrl+Shift+P") {
                val selected = context[SelectionDistributor].focusedView
                selected.now?.focus()
            }
        }
        box.setPrefSize(854.0, 480.0)
        return box
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(HaskEditorApplication::class.java)
        }
    }
}

