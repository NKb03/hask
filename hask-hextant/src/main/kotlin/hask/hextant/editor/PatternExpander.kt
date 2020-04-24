/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor

import hask.core.ast.Pattern
import hextant.Context
import hextant.Editor
import hextant.core.editor.ConfiguredExpander
import hextant.core.editor.ExpanderConfig

class PatternExpander(context: Context) : ConfiguredExpander<Pattern, Editor<Pattern>>(config, context) {
    companion object {
        val config = ExpanderConfig<Editor<Pattern>>().apply {
            registerConstant("_", ::OtherwisePatternEditor)
            registerConstant("otherwise", ::OtherwisePatternEditor)
            registerInterceptor { text, context ->
                text.toIntOrNull()?.let { IntegerPatternEditor(context, IntLiteralEditor(context, text)) }
            }
        }
    }
}