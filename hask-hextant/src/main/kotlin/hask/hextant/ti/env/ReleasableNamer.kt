/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti.env

import hask.core.type.Namer
import java.util.*

class ReleasableNamer: Namer {
    private var counter = 0

    private var char = 'a'

    private val released = TreeSet<String>()

    override fun freshName(): String {
        released.pollFirst()?.let {
            println("reusing $it")
            return@freshName it
        }
        val str = "$char$counter"
        if (char == 'z') {
            char = 'a'
            counter++
        } else char++
        return str
    }

    fun release(name: String) {
        released.add(name)
    }
}