package jp.trackrail.shopclipper.core

import jp.trackrail.shopclipper.todoist.TodoistProject
import jp.trackrail.shopclipper.todoist.TodoistSection

// Ported from the Chrome extension's lib.js (v2.0.0).

data class ProjectOption(val id: String, val name: String, val depth: Int, val label: String)

// Projects -> flat, hierarchy-ordered option list (Inbox first, archived out).
// A child whose parent is archived (or missing) becomes a root.
fun buildProjectOptions(projects: List<TodoistProject?> = emptyList()): List<ProjectOption> {
    val active = projects.filterNotNull().filter { it.id.isNotEmpty() && !it.isArchived }
    val ids = active.map { it.id }.toSet()
    val children = mutableMapOf<String, MutableList<TodoistProject>>()
    val roots = mutableListOf<TodoistProject>()
    for (p in active) {
        val parent = p.parentId
        if (parent != null && parent in ids) children.getOrPut(parent) { mutableListOf() } += p else roots += p
    }
    val out = mutableListOf<ProjectOption>()
    fun walk(list: List<TodoistProject>, depth: Int) {
        list.sortedBy(::projectOrder).forEach { p ->
            out += ProjectOption(p.id, p.name, depth, "　".repeat(depth) + p.name)
            walk(children[p.id].orEmpty(), depth + 1)
        }
    }
    walk(roots, 0)
    return out
}

private fun projectOrder(p: TodoistProject): Int = if (p.inboxProject) -1 else p.childOrder ?: p.order ?: 0

fun sortSections(sections: List<TodoistSection?> = emptyList()): List<TodoistSection> =
    sections.filterNotNull()
        .filter { it.id.isNotEmpty() && !it.isArchived }
        .sortedBy(::sectionOrder)

private fun sectionOrder(s: TodoistSection): Int = s.sectionOrder ?: s.order ?: 0
