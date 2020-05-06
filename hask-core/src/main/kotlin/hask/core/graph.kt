/**
 * @author Nikolaus Knop
 */

package hask.core

import java.util.*
import kotlin.math.min

typealias Graph = List<Collection<Int>>

private fun Graph.edgesFrom(vertex: Int) = get(vertex)

private fun Graph.edges(): Sequence<Pair<Int, Int>> =
    indices.asSequence().flatMap { u -> edgesFrom(u).asSequence().map { v -> u to v } }

data class CondensedGraph(val components: List<Collection<Int>>, val componentGraph: Graph)

private open class DFS(val graph: Graph) {
    var idx = 0
    val num = IntArray(graph.size) { -1 }
    val low = IntArray(graph.size)
}

private class Tarjan(graph: Graph) : DFS(graph) {
    val stack = Stack<Int>()
    val inStack = BooleanArray(graph.size)
    val components = mutableListOf<MutableList<Int>>()
    val component = IntArray(graph.size)
}

private fun Tarjan.tarjanDFS(u: Int) {
    num[u] = idx
    low[u] = num[u]
    idx++
    stack.push(u)
    inStack[u] = true
    for (v in graph.edgesFrom(u)) {
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
            val v = stack.pop()
            inStack[v] = false
            c.add(v)
            component[v] = components.size
        } while (v != u)
        components.add(c)
    }
}

private fun Tarjan.connectedComponents() {
    for (u in graph.indices) {
        if (num[u] == -1) tarjanDFS(u)
    }
    components
}

fun Graph.connectedComponents(): List<List<Int>> = with(Tarjan(this)) {
    connectedComponents()
    components
}

fun Graph.condense(): CondensedGraph = with(Tarjan(this)) {
    connectedComponents()
    val adj = List(components.size) { mutableSetOf<Int>() }
    for (u in indices) {
        for (v in edgesFrom(u)) {
            val c = component[u]
            val d = component[v]
            if (c != d) adj[c].add(d)
        }
    }
    CondensedGraph(components, adj)
}

fun Graph.topologicalSort(): List<Int>? {
    val inDegree = IntArray(size)
    for ((_, v) in edges()) inDegree[v]++
    val q = indices.filterTo(LinkedList()) { u -> inDegree[u] == 0 }
    val res = mutableListOf<Int>()
    while (q.isNotEmpty()) {
        val u = q.poll()
        res.add(u)
        for (v in edgesFrom(u)) {
            inDegree[v]--
            if (inDegree[v] == 0) q.offer(v)
        }
    }
    return res.takeIf { it.size == size }
}

fun CondensedGraph.topologicalSort(): List<Collection<Int>> {
    val ts = componentGraph.topologicalSort() ?: throw AssertionError("Condensed graph must have a topological sorting")
    return ts.map { i -> components[i] }
}
