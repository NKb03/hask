/**
 *@author Nikolaus Knop
 */

package hask.core.type

class SimpleNamer private constructor(private var counter: Int) : Namer {
    constructor() : this(0)

    override fun freshName(): String {
        val c = 'a' + (counter % 26)
        val n = counter / 26
        counter++
        return "$c$n"
    }
}