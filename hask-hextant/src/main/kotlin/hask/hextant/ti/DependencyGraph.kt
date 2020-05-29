/**
 *@author Nikolaus Knop
 */

package hask.hextant.ti

import hask.core.*
import hextant.*
import kollektion.Cache
import reaktive.event.unitEvent
import reaktive.list.ReactiveList
import reaktive.list.observeEach
import reaktive.set.ReactiveSet
import reaktive.set.asSet
import reaktive.set.binding.values
import reaktive.value.binding.map
import reaktive.value.now

class DependencyGraph(private val input: ReactiveList<Pair<EditorResult<String>, ReactiveSet<String>>>) {
    private val invalidate = unitEvent()
    val invalidated = invalidate.stream

    val boundVariables = input.asSet().map { (name, _) -> name.map { it.orNull() } }.values()

    private val observer = input.observeEach { (n, v) ->
        n.observe { _ -> invalidate() } and v.observeSet { ch -> if (ch.element in boundVariables.now) invalidate() }
    } and input.observe { _ -> invalidate() }
    private val vertices = Cache { computeVertices() }
    private val adjacencyList = Cache { computeAdjacencyList() }
    private val condensedGraph = Cache { adjacencyList.get().condense() }
    private val topologicalSorting = Cache { adjacencyList.get().topologicalSort() }

    private val topologicallySortedSCCs = Cache { condensedGraph.get().topologicalSort() }

    private fun invalidate() {
        vertices.invalidate()
        adjacencyList.invalidate()
        condensedGraph.invalidate()
        topologicalSorting.invalidate()
        topologicallySortedSCCs.invalidate()
        invalidate.fire()
    }

    private fun computeAdjacencyList(): List<List<Int>> {
        val index = vertices.get()
        val adj = List(input.now.size) { mutableListOf<Int>() }
        for (b in input.now) {
            b.first.now.ifOk { name ->
                for (fv in b.second.now) {
                    val u = index[fv]
                    val v = index.getValue(name)
                    if (u != null) adj[u].add(v)
                }
            }
        }
        return adj
    }

    private fun computeVertices(): Map<String, Int> {
        val index = mutableMapOf<String, Int>()
        for ((i, b) in input.now.withIndex()) {
            b.first.now.ifOk { name -> index[name] = i }
        }
        return index
    }

    fun topologicallySortedSCCs() = topologicallySortedSCCs.get()

    fun topologicalSort() = topologicalSorting.get()

    fun vertices() = vertices.get()

    fun hasCycle(source: Set<String>): Boolean {
        val v = vertices.get()
        val vertices = source.mapNotNull { name -> v[name] }
        val adj = adjacencyList.get()
        return adj.hasCycle(vertices)
    }
}