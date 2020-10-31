/**
 * @author Nikolaus Knop
 */

package hask.hextant.view

import bundles.Bundle
import hask.core.rt.evaluate
import hask.core.type.TopLevelEnv
import hask.hextant.context.HaskInternal
import hask.hextant.editor.*
import hask.hextant.editor.type.*
import hask.hextant.ti.env.TIContext
import hextant.codegen.ProvideImplementation
import hextant.command.line.CommandLine
import hextant.completion.CompletionStrategy
import hextant.completion.CompoundCompleter
import hextant.context.*
import hextant.core.Editor
import hextant.core.view.*
import hextant.core.view.ListEditorControl.*
import hextant.core.view.ListEditorControl.Orientation.Horizontal
import hextant.core.view.ListEditorControl.Orientation.Vertical
import hextant.fx.registerShortcuts
import hextant.fx.view
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType.INFORMATION
import javafx.scene.text.Text
import org.controlsfx.control.PopOver
import reaktive.value.now
import validated.ifInvalid

@ProvideImplementation(ControlFactory::class)
fun createControl(editor: IntLiteralEditor, arguments: Bundle) =
    TokenEditorControl(editor, arguments, styleClass = "int-literal")


@ProvideImplementation(ControlFactory::class)
fun createControl(editor: LambdaEditor, arguments: Bundle) = CompoundEditorControl(editor, arguments) {
    line {
        operator("λ")
        view(editor.parameters) {
            set(ListEditorControl.ORIENTATION, Horizontal)
            set(ListEditorControl.CELL_FACTORY) { DefaultCell() }
        }
        operator("->")
        view(editor.body)
    }
    styleClass.add("lambda")
}

@ProvideImplementation(ControlFactory::class)
fun createControl(editor: ApplyEditor, arguments: Bundle) = CompoundEditorControl(editor, arguments) {
    line {
        view(editor.applied)
        view(editor.arguments) {
            set(ListEditorControl.ORIENTATION, Horizontal)
            set(ListEditorControl.CELL_FACTORY) { DefaultCell() }
        }
    }
    styleClass.add("apply")
}

@ProvideImplementation(ControlFactory::class)
fun createControl(editor: LetEditor, arguments: Bundle) = CompoundEditorControl(editor, arguments) {
    line {
        keyword("let")
        space()
        view(editor.bindings)
    }
    line {
        keyword("in")
        space()
        view(editor.body)
    }
    styleClass.add("let")
}

@ProvideImplementation(ControlFactory::class)
fun createControl(editor: BindingEditor, arguments: Bundle) = CompoundEditorControl(editor, arguments) {
    line {
        view(editor.name)
        operator("=")
        view(editor.value)
    }
    styleClass.add("binding")
}

@ProvideImplementation(ControlFactory::class)
fun createControl(editor: BindingListEditor, arguments: Bundle): ListEditorControl {
    arguments[Companion.ORIENTATION] = Vertical
    return ListEditorControl.withAltText(editor, "Add binding", arguments)
}

@ProvideImplementation(ControlFactory::class)
fun createControl(editor: ExprExpander, arguments: Bundle) = run {
    val completer = CompoundCompleter<Editor<*>, Any>()
    completer.addCompleter(ReferenceCompleter)
    completer.addCompleter(FunctionApplicationCompleter)
    completer.addCompleter(ExprExpander.config.completer(CompletionStrategy.simple))
    ExpanderControl(editor, arguments, completer).apply {
        registerShortcuts {
            on("Ctrl+J") { editor.wrapInApply() }
            on("Ctrl+Shift+V") { editor.wrapInLet() }
            on("Ctrl+T") {
                val type = editor.type.now
                val ti = editor.context[HaskInternal, TIContext]
                val txt = ti.displayType(type)
                PopOver(Text(txt)).run {
                    isHideOnEscape = true
                    show(this@apply)
                }
            }
            on("Ctrl+Shift+T") {
                val t = editor.type.now
                PopOver(Text(t.toString())).run {
                    isHideOnEscape = true
                    show(this@apply)
                }
            }
            on("Ctrl+E") {
                editor.evaluateOnce()
            }
            on("Ctrl+Shift+E") {
                editor.evaluateFully()
            }
            on("Ctrl+U") {
                editor.unevaluate()
            }
            on("Ctrl+Shift+U") {
                editor.unevaluateFully()
            }
        }
    }
}

@ProvideImplementation(ControlFactory::class)
fun createControl(editor: PatternExpander, arguments: Bundle): ExpanderControl {
    val completer = CompoundCompleter<Editor<*>, Any>()
    completer.addCompleter(PatternExpander.config.completer(CompletionStrategy.simple))
    completer.addCompleter(DestructuringPatternCompleter)
    return ExpanderControl(editor, arguments, completer)
}

@ProvideImplementation(ControlFactory::class)
fun createControl(editor: IntegerPatternEditor, arguments: Bundle) = CompoundEditorControl(editor, arguments) {
    view(editor.value)
    styleClass.add("integer-pattern")
}

@ProvideImplementation(ControlFactory::class)
fun createControl(editor: VariablePatternEditor, arguments: Bundle) = CompoundEditorControl(editor, arguments) {
    view(editor.identifier)
}

@ProvideImplementation(ControlFactory::class)
fun createControl(editor: DestructuringPatternEditor, arguments: Bundle) = CompoundEditorControl(editor, arguments) {
    line {
        view(editor.constructor).root.styleClass.add("adt-constructor-name")
        view(editor.arguments) {
            set(Companion.CELL_FACTORY) { SeparatorCell("") }
            set(Companion.ORIENTATION, Horizontal)
        }
    }
}

@ProvideImplementation(ControlFactory::class)
fun createControl(editor: WildcardPatternEditor, arguments: Bundle) = CompoundEditorControl(editor, arguments) {
    operator("_")
    styleClass.add("wildcard-pattern")
}

@ProvideImplementation(ControlFactory::class)
fun createControl(editor: CaseEditor, arguments: Bundle) = CompoundEditorControl(editor, arguments) {
    line {
        view(editor.pattern)
        operator(" -> ")
        view(editor.body)
    }
    styleClass.add("case")
}

@ProvideImplementation(ControlFactory::class)
fun createControl(editor: MatchEditor, arguments: Bundle) = CompoundEditorControl(editor, arguments) {
    line {
        keyword("match")
        space()
        view(editor.matched)
        keyword("with")
    }
    indented {
        view(editor.cases) {
            set(Companion.ORIENTATION, Vertical)
        }
    }
    styleClass.add("match")
}

@ProvideImplementation(ControlFactory::class)
fun createControl(editor: IfEditor, arguments: Bundle) = CompoundEditorControl(editor, arguments) {
    line {
        keyword("if")
        space()
        view(editor.condition)
        space()
        keyword("then")
        space()
        view(editor.ifTrue)
    }
    line {
        keyword("else")
        space()
        view(editor.ifFalse)
    }
    styleClass.add("if")
}

@ProvideImplementation(ControlFactory::class)
fun createControl(editor: TypeExpander, arguments: Bundle): ExpanderControl {
    val completer = CompoundCompleter<Editor<*>, Any>()
    completer.addCompleter(SimpleTypeCompleter)
    completer.addCompleter(ParameterizedADTCompleter)
    completer.addCompleter(TypeExpander.config.completer(CompletionStrategy.simple))
    return ExpanderControl(editor, arguments, completer)
}

@ProvideImplementation(ControlFactory::class)
fun createControl(editor: SimpleTypeEditor, arguments: Bundle) =
    EditorControlWrapper(editor, editor.context.createControl(editor.name), arguments)

@ProvideImplementation(ControlFactory::class)
fun createControl(editor: FuncTypeEditor, arguments: Bundle) = CompoundEditorControl(editor, arguments) {
    line {
        operator("(")
        view(editor.parameterType)
        operator(" -> ")
        view(editor.resultType)
        operator(")")
    }
}

@ProvideImplementation(ControlFactory::class)
fun createControl(editor: TypeListEditor, arguments: Bundle): ListEditorControl {
    arguments[Companion.ORIENTATION] = Horizontal
    return ListEditorControl(editor, arguments)
}

@ProvideImplementation(ControlFactory::class)
fun createControl(editor: TypeSchemeEditor, arguments: Bundle) = CompoundEditorControl(editor, arguments) {
    line {
        operator("Ɐ")
        view(editor.parameters)
        operator(" . ")
        view(editor.body)
    }
}

@ProvideImplementation(ControlFactory::class)
fun createControl(editor: ADTEditor, arguments: Bundle) = CompoundEditorControl(editor, arguments) {
    line {
        keyword("data ")
        view(editor.name).root.styleClass.add("adt-name")
        view(editor.parameters) {
            set(Companion.ORIENTATION, Horizontal)
        }
    }
    styleClass.add("adt")
}

@ProvideImplementation(ControlFactory::class)
fun createControl(editor: ADTConstructorEditor, arguments: Bundle) = CompoundEditorControl(editor, arguments) {
    line {
        view(editor.name).root.styleClass.add("adt-constructor-name")
        space()
        view(editor.parameters) {
            set(Companion.ORIENTATION, Horizontal)
        }
    }
    styleClass.add("adt-constructor")
}

@ProvideImplementation(ControlFactory::class)
fun createControl(editor: ADTDefEditor, arguments: Bundle) = CompoundEditorControl(editor, arguments) {
    line {
        view(editor.adt)
        operator("=")
        view(editor.constructors) {
            set(Companion.ORIENTATION, Horizontal)
            set(Companion.CELL_FACTORY) { SeparatorCell(" | ") }
        }
    }
    styleClass.add("adt-def")
}

@ProvideImplementation(ControlFactory::class)
fun createControl(editor: ProgramEditor, arguments: Bundle) = CompoundEditorControl(editor, arguments) {
    val context = editor.context
    val ti = context[HaskInternal, TIContext]
    val unificator = ti.unificator
    editor.expr.inference.activate()
    setPrefSize(854.0, 480.0)
    view(editor.adtDefs) { set(Companion.ORIENTATION, Vertical) }
    view(editor.expr)
    view(context[CommandLine])
    registerShortcuts {
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
            context[EditorControlGroup].getViewOf(context[CommandLine]).receiveFocus()
        }
        on("Ctrl+Shift+P") {
            val selected = context[SelectionDistributor].focusedView
            selected.now?.focus()
        }
    }
}

@ProvideImplementation(ControlFactory::class)
fun createControl(editor: ParameterizedADTEditor, arguments: Bundle) = CompoundEditorControl(editor, arguments) {
    line {
        view(editor.name) {
            set(AbstractTokenEditorControl.COMPLETER, ParameterizedADTCompleter)
        }.root.styleClass.add("parameterized-adt-name")
        space()
        view(editor.typeArguments) { set(Companion.ORIENTATION, Horizontal) }
    }
    styleClass.add("parameterized-adt")
}

@ProvideImplementation(ControlFactory::class)
fun createControl(editor: StringEditor, arguments: Bundle) = TokenEditorControl(editor, arguments)