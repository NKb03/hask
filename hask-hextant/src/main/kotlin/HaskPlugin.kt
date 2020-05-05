import hask.hextant.*
import hask.hextant.context.HaskInternal
import hask.hextant.editor.*
import hask.hextant.editor.type.*
import hask.hextant.ti.env.TIContext
import hask.hextant.view.IdentifierEditorControl
import hask.hextant.view.ValueOfEditorControl
import hextant.*
import hextant.base.CompoundEditorControl
import hextant.base.view
import hextant.bundle.createBundle
import hextant.completion.NoCompleter
import hextant.core.view.*
import hextant.core.view.ListEditorControl.*
import hextant.core.view.ListEditorControl.Companion.CELL_FACTORY
import hextant.core.view.ListEditorControl.Companion.ORIENTATION
import hextant.core.view.ListEditorControl.Orientation.Horizontal
import hextant.fx.ModifierValue.DOWN
import hextant.fx.registerShortcuts
import hextant.fx.shortcut
import hextant.plugin.dsl.PluginInitializer
import javafx.scene.input.KeyCode.*
import javafx.scene.text.Text
import org.controlsfx.control.PopOver
import reaktive.value.now

object HaskPlugin : PluginInitializer({
    name = "Hextant Hask Plugin"
    author = "Nikolaus Knop"
    defaultEditor(::IdentifierEditor)
    defaultEditor(::IntLiteralEditor)
    defaultEditor(::ValueOfEditor)
    defaultEditor(::ApplyEditor)
    defaultEditor(::LambdaEditor)
    defaultEditor(::LetEditor)
    defaultEditor(::ExprExpander)

    view(::IdentifierEditorControl)
    view { e: IntLiteralEditor, bundle ->
        FXTokenEditorView(e, bundle, NoCompleter).apply {
            root.styleClass.add("int-literal")
        }
    }
    view { e: ValueOfEditor, bundle ->
        ValueOfEditorControl(e, bundle)
    }
    view { e: ApplyEditor, bundle ->
        CompoundEditorControl.build(e, bundle) {
            line {
                view(e.applied)
                view(e.arguments) {
                    set(ORIENTATION, Horizontal)
                    set(CELL_FACTORY) { SeparatorCell("") }
                }
            }
            styleClass.add("apply")
        }
    }
    view { e: LambdaEditor, bundle ->
        CompoundEditorControl.build(e, bundle) {
            line {
                operator("λ")
                view(e.parameters)
                operator("->")
                view(e.body)
            }
            styleClass.add("lambda")
        }
    }
    view { e: LetEditor, bundle ->
        CompoundEditorControl.build(e, bundle) {
            line {
                keyword("let")
                space()
                view(e.bindings)
            }
            line {
                keyword("in")
                space()
                view(e.body)
            }
            styleClass.add("let")
        }
    }
    compoundView { e: BindingEditor ->
        line {
            view(e.name)
            operator("=")
            view(e.value)
        }
    }
    view { e: BindingListEditor, args ->
        args[ORIENTATION] = Orientation.Vertical
        ListEditorControl.withAltText(e, "Add binding", args)
    }
    view { e: ExprExpander, bundle ->
        FXExpanderView(e, bundle, ExprCompleter).apply {
            registerShortcuts {
                on(shortcut(U) { control(DOWN) }) { e.wrapInApply() }
                on(shortcut(V) { control(DOWN); alt(DOWN) }) { e.wrapInLet() }
                on(shortcut(T) { control(DOWN) }) {
                    val type = e.type.now
                    val ti = e.context[HaskInternal, TIContext]
                    val txt = ti.displayType(type)
                    PopOver(Text(txt)).run {
                        isHideOnEscape = true
                        show(this@apply)
                    }
                }
                on("Ctrl+Shift+T") {
                    val t = e.type.now.ifErr { return@on }
                    PopOver(Text(t.toString())).run {
                        isHideOnEscape = true
                        show(this@apply)
                    }
                }
                //                on(shortcut(E) { control(DOWN) }) {
                //                    e.evaluateOnce()
                //                }
                //                on(shortcut(E) { control(DOWN); shift(DOWN) }) {
                //                    e.evaluateFully()
                //                }
                //                on(shortcut(U) { control(DOWN) }) {
                //                    e.unevaluate()
                //                }
                //                on(shortcut(U) { control(DOWN); shift(DOWN) }) {
                //                    e.unevaluateFully()
                //                }
            }
        }
    }
    view { e: IntegerPatternEditor, bundle ->
        CompoundEditorControl.build(e, bundle) {
            view(e.value)
        }
    }
    view { e: OtherwisePatternEditor, bundle ->
        CompoundEditorControl.build(e, bundle) {
            operator("_")
        }
    }
    view { e: CaseEditor, bundle ->
        CompoundEditorControl.build(e, bundle) {
            line {
                view(e.pattern)
                operator(" -> ")
                view(e.body)
            }
        }
    }
    view { e: CaseListEditor, bundle ->
        ListEditorControl(e, bundle)
    }
    view { e: MatchEditor, bundle ->
        CompoundEditorControl.build(e, bundle) {
            line {
                keyword("match")
                view(e.matched)
                keyword("with")
            }
            indented {
                view(e.cases)
            }
        }
    }
    view { e: IfEditor, bundle ->
        CompoundEditorControl.build(e, bundle) {
            line {
                keyword("if")
                space()
                view(e.condition)
                space()
                keyword("then")
                space()
                view(e.ifTrue)
            }
            line {
                keyword("else")
                space()
                view(e.ifFalse)
            }
        }
    }
    view { e: SimpleTypeEditor, bundle -> EditorControlWrapper(e, e.context.createView(e.name), bundle) }
    view { e: FuncTypeEditor, bundle ->
        CompoundEditorControl.build(e, bundle) {
            line {
                operator("(")
                view(e.parameterType)
                operator(" -> ")
                view(e.resultType)
                operator(")")
            }
        }
    }
    view { e: IdentifierListEditor, bundle ->
        bundle[ORIENTATION] = Horizontal
        bundle[CELL_FACTORY] = { SeparatorCell("") }
        ListEditorControl(e, bundle)
    }
    view { e: TypeSchemeEditor, bundle ->
        CompoundEditorControl.build(e, bundle) {
            line {
                operator("Ɐ")
                view(e.parameters)
                operator(" . ")
                view(e.body)
            }
        }
    }
    inspection(::typeOkInspection)
    inspection(::typeConstraintInspection)
//    inspection(::betaConversion)
    stylesheet("hextant/hask/style.css")
})