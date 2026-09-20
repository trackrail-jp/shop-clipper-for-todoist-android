package jp.trackrail.shopclipper.core

import jp.trackrail.shopclipper.todoist.TodoistProject
import jp.trackrail.shopclipper.todoist.TodoistSection
import org.junit.Assert.assertEquals
import org.junit.Test

// Ported from the extension's test/lib.test.js (project / section lists).
class ProjectOptionsTest {
    @Test
    fun buildProjectOptionsOrdersInboxFirstNestsChildrenSkipsArchived() {
        val projects = listOf(
            TodoistProject(id = "shop", name = "🛍 買い物", childOrder = 3),
            TodoistProject(id = "cand", name = "🛒 ほしいもの", parentId = "shop", childOrder = 1),
            TodoistProject(id = "done", name = "✅ 買ったもの", parentId = "shop", childOrder = 2),
            TodoistProject(id = "inbox", name = "Inbox", inboxProject = true, childOrder = 5),
            TodoistProject(id = "old", name = "旧", isArchived = true, childOrder = 0),
            TodoistProject(id = "orphan", name = "親がアーカイブ", parentId = "old", order = 9),
            TodoistProject(id = "plain", name = "Plain"),
            TodoistProject(id = "", name = "id なし"),
            null,
        )
        val opts = buildProjectOptions(projects)
        assertEquals(
            listOf("inbox" to 0, "plain" to 0, "shop" to 0, "cand" to 1, "done" to 1, "orphan" to 0),
            opts.map { it.id to it.depth },
        )
        assertEquals("　🛒 ほしいもの", opts[3].label)
        assertEquals(emptyList<ProjectOption>(), buildProjectOptions())
    }

    @Test
    fun sortSectionsOrdersBySectionOrderOrOrderAndSkipsArchived() {
        val out = sortSections(
            listOf(
                TodoistSection(id = "b", name = "10", sectionOrder = 2),
                TodoistSection(id = "a", name = "00", sectionOrder = 1),
                TodoistSection(id = "c", name = "20", order = 3),
                TodoistSection(id = "x", name = "old", sectionOrder = 0, isArchived = true),
                TodoistSection(id = "z", name = "none"),
                TodoistSection(id = "", name = "id なし"),
                null,
            ),
        )
        assertEquals(listOf("z", "a", "b", "c"), out.map { it.id })
        assertEquals(emptyList<TodoistSection>(), sortSections())
    }
}
