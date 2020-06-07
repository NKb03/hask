/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Pattern
import hask.core.ast.Pattern.Wildcard
import hask.core.parse.IDENTIFIER_REGEX
import hextant.Context
import hextant.core.editor.ConfiguredExpander
import hextant.core.editor.ExpanderConfig
import reaktive.set.ReactiveSet
import reaktive.set.binding.flattenToSet
import reaktive.set.emptyReactiveSet
import reaktive.value.binding.map
import validated.Validated
import validated.valid

class PatternExpander(context: Context) : ConfiguredExpander<Pattern, PatternEditor<*>>(config, context),
                                          PatternEditor<Pattern> {
    override fun defaultResult(): Validated<Pattern> = valid(Wildcard)

    override val boundVariables: ReactiveSet<String> =
        editor.map { it?.boundVariables ?: emptyReactiveSet() }.flattenToSet()

    companion object {
        val config = ExpanderConfig<PatternEditor<*>>().apply {
            registerConstant("_", ::WildcardPatternEditor)
            registerConstant("wildcard", ::WildcardPatternEditor)
            registerConstant("otherwise", ::WildcardPatternEditor)
            registerConstant("destructuring", ::DestructuringPatternEditor)
            registerConstant("var", ::VariablePatternEditor)
            registerInterceptor { text, context ->
                if (text.matches(IDENTIFIER_REGEX)) VariablePatternEditor(context, text)
                else null
            }
            registerInterceptor { text, context ->
                text.toIntOrNull()?.let { IntegerPatternEditor(context, text) }
            }
        }
    }
}