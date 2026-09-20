package jp.trackrail.shopclipper.todoist

import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelsTest {
    @Test
    fun taskPayloadLeavesNullFieldsOut() {
        assertEquals(
            """{"content":"c","priority":1}""",
            TodoistJson.encodeToString(TaskPayload.serializer(), TaskPayload(content = "c", priority = 1)),
        )
        assertEquals(
            """{"content":"c","priority":4,"description":"d","project_id":"P","section_id":"S","labels":["Shopping_Amazon"]}""",
            TodoistJson.encodeToString(
                TaskPayload.serializer(),
                TaskPayload("c", 4, "d", "P", "S", listOf("Shopping_Amazon")),
            ),
        )
    }

    @Test
    fun responsesIgnoreUnknownKeysAndNulls() {
        val project = TodoistJson.decodeFromString(
            TodoistProject.serializer(),
            """{"id":"6X","name":"Inbox","parent_id":null,"child_order":0,"inbox_project":true,"color":"grey","is_archived":null}""",
        )
        assertEquals(TodoistProject(id = "6X", name = "Inbox", childOrder = 0, inboxProject = true), project)
        val section = TodoistJson.decodeFromString(TodoistSection.serializer(), """{"id":"S1","name":"x","section_order":2}""")
        assertEquals(TodoistSection(id = "S1", name = "x", sectionOrder = 2), section)
        val task = TodoistJson.decodeFromString(TodoistTask.serializer(), """{"id":"T","content":"c","priority":4}""")
        assertEquals("", task.description)
        // Round trip keeps the declared fields.
        val json = TodoistJson.encodeToString(TodoistProject.serializer(), project)
        assertEquals(project, TodoistJson.decodeFromString(TodoistProject.serializer(), json))
        assertEquals("Inbox", TodoistJson.parseToJsonElement(json).jsonObject["name"].toString().trim('"'))
    }
}
