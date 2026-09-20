package jp.trackrail.shopclipper.todoist

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Todoist API v1 shapes. Only the fields this app reads or writes are declared;
// everything else in a response is ignored (TodoistJson.ignoreUnknownKeys).

/** Body of POST /tasks. A null field is left out of the JSON (TodoistJson.explicitNulls). */
@Serializable
data class TaskPayload(
    val content: String,
    val priority: Int,
    val description: String? = null,
    @SerialName("project_id") val projectId: String? = null,
    @SerialName("section_id") val sectionId: String? = null,
    val labels: List<String>? = null,
)

@Serializable
data class TodoistProject(
    val id: String = "",
    val name: String = "",
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("child_order") val childOrder: Int? = null,
    val order: Int? = null,
    @SerialName("inbox_project") val inboxProject: Boolean = false,
    @SerialName("is_archived") val isArchived: Boolean = false,
)

@Serializable
data class TodoistSection(
    val id: String = "",
    val name: String = "",
    @SerialName("section_order") val sectionOrder: Int? = null,
    val order: Int? = null,
    @SerialName("is_archived") val isArchived: Boolean = false,
)

@Serializable
data class TodoistTask(
    val id: String = "",
    val content: String = "",
    val description: String = "",
)

/** One Json for requests and responses: lenient about fields we do not know. */
val TodoistJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
}
