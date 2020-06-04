import hask.hextant.*
import hask.hextant.context.HaskInternal
import hask.hextant.editor.*
import hask.hextant.editor.type.*
import hask.hextant.ti.env.TIContext
import hask.hextant.view.IdentifierEditorControl
import hask.hextant.view.ValueOfEditorControl
import hextant.*
import hextant.command.Command.Type.SingleReceiver
import hextant.completion.*
import hextant.core.view.*
import hextant.core.view.ListEditorControl.Companion.CELL_FACTORY
import hextant.core.view.ListEditorControl.Companion.ORIENTATION
import hextant.core.view.ListEditorControl.Orientation.Horizontal
import hextant.core.view.ListEditorControl.Orientation.Vertical
import hextant.core.view.ListEditorControl.SeparatorCell
import hextant.fx.registerShortcuts
import hextant.fx.view
import hextant.inspect.Inspection
import hextant.plugin.dsl.PluginInitializer
import javafx.scene.text.Text
import org.controlsfx.control.PopOver
import reaktive.value.now
import java.nio.file.Paths

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
        FXTokenEditorView(e, bundle).apply {
            root.styleClass.add("int-literal")
        }
    }
    view { e: ValueOfEditor, bundle -> ValueOfEditorControl(e, bundle) }
    compoundView { e: ApplyEditor ->
        line {
            view(e.applied)
            view(e.arguments) {
                set(ORIENTATION, Horizontal)
                set(CELL_FACTORY) { SeparatorCell("") }
            }
        }
        styleClass.add("apply")
    }
    compoundView { e: LambdaEditor ->
        line {
            operator("λ")
            view(e.parameters)
            operator("->")
            view(e.body)
        }
        styleClass.add("lambda")
    }
    compoundView { e: LetEditor ->
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
    compoundView { e: BindingEditor ->
        line {
            view(e.name)
            operator("=")
            view(e.value)
        }
        styleClass.add("binding")
    }
    view { e: BindingListEditor, args ->
        args[ORIENTATION] = Vertical
        ListEditorControl.withAltText(e, "Add binding", args)
    }
    view { e: ExprExpander, bundle ->
        val completer = CompoundCompleter<Context, Any?>()
        completer.addCompleter(ReferenceCompleter)
        completer.addCompleter(KeywordExprCompleter)
        FXExpanderView(e, bundle, completer).apply {
            registerShortcuts {
                on("Ctrl+SPACE") { e.wrapInApply() }
                on("Ctrl+Shift+V") { e.wrapInLet() }
                on("Ctrl+T") {
                    val type = e.type.now
                    val ti = e.context[HaskInternal, TIContext]
                    val txt = ti.displayType(type)
                    PopOver(Text(txt)).run {
                        isHideOnEscape = true
                        show(this@apply)
                    }
                }
                on("Ctrl+Shift+T") {
                    val t = e.type.now
                    PopOver(Text(t.toString())).run {
                        isHideOnEscape = true
                        show(this@apply)
                    }
                }
                on("Ctrl+E") {
                    e.evaluateOnce()
                }
                on("Ctrl+Shift+E") {
                    e.evaluateFully()
                }
                on("Ctrl+U") {
                    e.unevaluate()
                }
                on("Ctrl+Shift+U") {
                    e.unevaluateFully()
                }
            }
        }
    }
    compoundView { e: IntegerPatternEditor ->
        view(e.value)
        styleClass.add("integer-pattern")
    }
    compoundView { e: OtherwisePatternEditor ->
        operator("_")
        styleClass.add("otherwise")
    }
    compoundView { e: CaseEditor ->
        line {
            view(e.pattern)
            operator(" -> ")
            view(e.body)
        }
        styleClass.add("case")
    }
    view { e: CaseListEditor, bundle ->
        ListEditorControl(e, bundle)
    }
    compoundView { e: MatchEditor ->
        line {
            keyword("match")
            view(e.matched)
            keyword("with")
        }
        indented {
            view(e.cases)
        }
        styleClass.add("match")
    }
    compoundView { e: IfEditor ->
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
        styleClass.add("if")
    }
    view { e: TypeExpander, bundle ->
        val completer = CompoundCompleter<Context, Any>()
        completer.addCompleter(SimpleTypeCompleter)
        completer.addCompleter(ParameterizedADTCompleter)
        completer.addCompleter(TypeExpander.config.completer(CompletionStrategy.simple))
        FXExpanderView(e, bundle, completer)
    }
    view { e: SimpleTypeEditor, bundle -> EditorControlWrapper(e, e.context.createView(e.name), bundle) }
    compoundView { e: FuncTypeEditor ->
        line {
            operator("(")
            view(e.parameterType)
            operator(" -> ")
            view(e.resultType)
            operator(")")
        }
    }
    view { e: IdentifierListEditor, bundle ->
        bundle[ORIENTATION] = Horizontal
        ListEditorControl(e, bundle)
    }
    compoundView { e: TypeSchemeEditor ->
        line {
            operator("Ɐ")
            view(e.parameters)
            operator(" . ")
            view(e.body)
        }
    }
    compoundView { e: ADTEditor ->
        line {
            keyword("data ")
            view(e.name).root.styleClass.add("adt-name")
            view(e.parameters) {
                set(ORIENTATION, Horizontal)
            }
        }
        styleClass.add("adt")
    }
    compoundView { e: ADTConstructorEditor ->
        line {
            view(e.name).root.styleClass.add("adt-constructor-name")
            space()
            view(e.parameters) {
                set(ORIENTATION, Horizontal)
            }
        }
        styleClass.add("adt-constructor")
    }
    compoundView { e: ADTDefEditor ->
        line {
            view(e.adt)
            operator("=")
            view(e.constructors) {
                set(ORIENTATION, Horizontal)
                set(CELL_FACTORY) { SeparatorCell(" | ") }
            }
        }
        styleClass.add("adt-def")
    }
    compoundView { e: ProgramEditor ->
        view(e.adtDefs) { set(ORIENTATION, Vertical) }
        view(e.expr)
    }
    compoundView { e: ParameterizedADTEditor ->
        line {
            view(e.name) {
                set(AbstractTokenEditorControl.COMPLETER, ParameterizedADTCompleter)
            }.root.styleClass.add("parameterized-adt-name")
            keyword(" of ")
            view(e.typeArguments) { set(ORIENTATION, Horizontal) }
        }
        styleClass.add("parameterized-adt")
    }
    command(eval)
    registerCommand<ExprExpander, Unit> {
        name = "unevaluate"
        shortName = "uneval"
        description = "Undoes a previous step of computation"
        type = SingleReceiver
        executing { e, _ -> e.unevaluate() }
    }
    registerCommand<ExprExpander, Unit> {
        name = "unevaluate fully"
        shortName = "uneval!"
        description = "Undoes all previous steps of computation"
        type = SingleReceiver
        executing { e, _ -> e.unevaluateFully() }
    }
    registerCommand<ExprEditor<*>, String> {
        name = "free variables"
        shortName = "fvs"
        description = "Prints all free variables of the expression"
        type = SingleReceiver
        executing { e, _ -> e.freeVariables.now.toString() }
    }
    registerCommand<ExprExpander, String> {
        name = "save"
        shortName = "save"
        description = "Saves the selected editor to the specified file"
        type = SingleReceiver
        addParameter {
            ofType<String>()
            name = "file"
            description = "The destination file"
        }
        applicableIf { it.isExpanded }
        executing { expander, args ->
            val file = args[0] as String
            val path = Paths.get(file)
            val output = expander.context.createOutput(path)
            try {
                output.writeUntyped(expander)
                "Successfully saved to $path"
            } catch (ex: Throwable) {
                ex.message?.let { "Failure: $it" } ?: "Unknown error"
            }
        }
    }
    registerCommand<ExprExpander, String> {
        name = "open"
        shortName = "open"
        description = "Reads the selected editor from the specified file"
        type = SingleReceiver
        addParameter {
            ofType<String>()
            name = "file"
            description = "The input file"
        }
        applicableIf { !it.isExpanded }
        executing { expander, args ->
            val file = args[0] as String
            val path = Paths.get(file)
            val input = expander.context.createInput(path)
            try {
                input.readInplace(expander)
                "Successfully read from $path"
            } catch (ex: Throwable) {
                ex.message?.let { "Failure: $it" } ?: "Unknown error"
            }
        }
    }
    inspection(::unresolvedVariableInspection)
    inspection(::typeParameterUnresolvedInspection)
    inspection(::unresolvedADTInspection)
    inspection<IdentifierEditor, Inspection>(::invalidIdentifierInspection)
    inspection<ValueOfEditor, Inspection>(::invalidIdentifierInspection)
    inspection(::invalidIntLiteralInspection)
    inspection(::typeConstraintInspection)
    inspection(::betaConversion)
    stylesheet("hextant/hask/style.css")
})