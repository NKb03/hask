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
    private val used = mutableSetOf<String>()

    override fun freshName(): String {
        released.pollFirst()?.let {
            used.add(it)
            //println("using $it")
            return@freshName it
        }
        val str = "$char$counter"
        if (char == 'z') {
            char = 'a'
            counter++
        } else char++
        used.add(str)
        //println("using $str")
        return str
    }

    override fun toString(): String = buildString {
        append("Used ")
        used.joinTo(this, postfix = "\n")
        append("Available: ")
        released.joinTo(this, postfix = "\n")
        append("Next: $char$counter")
    }

    fun release(name: String) {
        //println("Releasing $name")
        if (!used.remove(name)) System.err.println("Never used $name")
        if (!released.add(name)) System.err.println("Already released $name")
    }
}