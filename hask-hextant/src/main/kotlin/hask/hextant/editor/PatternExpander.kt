/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Pattern
import hask.core.ast.Pattern.Wildcard
import hask.core.parse.IDENTIFIER_REGEX
import hask.hextant.ti.env.ADTDefinitions
import hextant.Context
import hextant.core.editor.ConfiguredExpander
import hextant.core.editor.ExpanderConfig
import reaktive.set.ReactiveSet
import reaktive.set.binding.flattenToSet
import reaktive.set.emptyReactiveSet
import reaktive.value.binding.map
import reaktive.value.now
import validated.Validated
import validated.valid

class PatternExpander(context: Context) : ConfiguredExpander<Pattern, PatternEditor<*>>(config, context),
                                          PatternEditor<Pattern> {
    override fun defaultResult(): Validated<Pattern> = valid(Wildcard)

    override val boundVariables: ReactiveSet<String> =
        editor.map { it?.boundVariables ?: emptyReactiveSet() }.flattenToSet()

    override fun onExpansion(editor: PatternEditor<*>) {
        if (editor is DestructuringPatternEditor && editor.arguments.editors.now.isNotEmpty()) {
            views {
                val arg = editor.arguments.editors.now.first()
                val v = group.getViewOf(arg)
                v.focus()
            }
        }
    }

    companion object {
        val config = ExpanderConfig<PatternEditor<*>>().apply {
            registerConstant("_", ::WildcardPatternEditor)
            registerConstant("wildcard", ::WildcardPatternEditor)
            registerConstant("otherwise", ::WildcardPatternEditor)
            registerConstant("destructuring", ::DestructuringPatternEditor)
            registerConstant("var", ::VariablePatternEditor)
            registerInterceptor { text, context ->
                if (!text.matches(IDENTIFIER_REGEX)) return@registerInterceptor null
                if (text.first().isUpperCase()) {
                    val (_, cstr) = context[ADTDefinitions].getInfo(text).now ?: return@registerInterceptor null
                    val e = DestructuringPatternEditor(context, text)
                    e.arguments.resize(cstr.parameters.size)
                    e
                }
                else VariablePatternEditor(context, text)
            }
            registerInterceptor { text, context ->
                text.toIntOrNull()?.let { IntegerPatternEditor(context, text) }
            }
        }
    }
}