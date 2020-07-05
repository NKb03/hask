/**
 *@author Nikolaus Knop
 */

package hask.hextant.editor.type

import hask.core.type.Type
import hextant.context.Context
import hextant.core.editor.ListEditor

class TypeListEditor(context: Context) : ListEditor<Type, TypeExpander>(context) {
    override fun createEditor(): TypeExpander? = TypeExpander(context)
}