/**
 *@author Nikolaus Knop
 */

package hask.hextant

import hask.hextant.editor.ProgramEditor
import hextant.test.HextantTestApplication

class HaskEditorApplication : HextantTestApplication(ProgramEditor) {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch<HaskEditorApplication>()
        }
    }
}

