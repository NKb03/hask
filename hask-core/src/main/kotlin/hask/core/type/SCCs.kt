/**
 * @author Nikolaus Knop
 */

package hask.core.type

import java.util.*
import kotlin.math.min

class SCCs(private val graph: Array<out List<Int>>) {
    private var idx = 0
    private val s = Stack<Int>()
    private val num = IntArray(graph.size) { -1 }
    private val low = IntArray(graph.size)
    private val inStack = BooleanArray(graph.size)
    private val components = mutableListOf<MutableList<Int>>()
    private val component = IntArray(graph.size)

    private fun tarjanDFS(u: Int) {
        num[u] = idx
        low[u] = num[u]
        idx++
        s.push(u)
        inStack[u] = true
        for (v in graph[u]) {
            if (num[v] == -1) {
                tarjanDFS(v)
                low[u] = min(low[u], low[v])
            } else if (inStack[v]) {
                low[u] = min(low[u], num[v])
            }
        }
        if (low[u] == num[u]) {
            val c = mutableListOf<Int>()
            do {
                val v = s.pop()
                inStack[v] = false
                c.add(v)
                component[v] = components.size
            } while (v != u)
            components.add(c)
        }
    }

    fun compute() {
        for (u in graph.indices) {
            if (num[u] == -1) tarjanDFS(u)
        }
    }

    fun topologicalSort(): List<List<Int>> {
        val incoming = Array(components.size) { mutableSetOf<Int>() }
        for (u in graph.indices) {
            for (v in graph[u]) {
                if (component[v] != component[u]) incoming[component[v]].add(u)
            }
        }
        val s = components.indices.filterTo(LinkedList()) { u -> incoming[u].isEmpty() }
        val res = mutableListOf<List<Int>>()
        while (s.isNotEmpty()) {
            val c = s.poll()
            res.add(components[c])
            for (u in components[c]) {
                for (v in graph[u]) {
                    val d = component[v]
                    if (d == c) continue
                    incoming[d].remove(u)
                    if (incoming[d].isEmpty()) s.offer(d)
                }
            }
        }
        return res
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val graph = arrayOf(listOf(1, 2), listOf(0), listOf(3), listOf(2))
            val sccs = SCCs(graph)
            sccs.compute()
            println(sccs.topologicalSort())
        }
    }
}
