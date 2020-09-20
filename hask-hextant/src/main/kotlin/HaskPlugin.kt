import hask.hextant.*
import hask.hextant.editor.*
import hextant.command.Command.Type.SingleReceiver
import hextant.plugin.*

object HaskPlugin : PluginInitializer({
    registerCommand(eval)
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
    registerCommand<ExprEditor<*>>(save)
    registerCommand<ExprEditor<*>>(open)
    registerCommand<ADTDefEditor>(save)
    registerCommand<ADTDefEditor>(open)
    inspection(unresolvedVariableInspection)
    inspection(typeParameterUnresolvedInspection)
    inspection(unresolvedADTInspection)
    inspection<IdentifierEditor>(invalidIdentifierInspection)
    inspection<ValueOfEditor>(invalidIdentifierInspection)
    inspection(invalidIntLiteralInspection)
    inspection(typeConstraintInspection)
    inspection(betaConversion)
    stylesheet("hextant/hask/style.css")
})