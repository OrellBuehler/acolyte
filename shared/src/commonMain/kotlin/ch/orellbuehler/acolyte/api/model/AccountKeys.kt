package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiKeyList(
    val keys: List<ApiKeyInfo> = emptyList(),
)

@Serializable
data class ApiKeyInfo(
    val id: Long = 0,
    val label: String = "",
    val hint: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("last_used_at") val lastUsedAt: String? = null,
    val scopes: List<String> = emptyList(),
    @SerialName("mcp_groups") val mcpGroups: List<String> = emptyList(),
)

@Serializable
internal data class NewApiKey(
    val label: String,
    val scopes: List<String>? = null,
    @SerialName("mcp_groups") val mcpGroups: List<String>? = null,
)

@Serializable
data class MadeApiKey(
    val id: Long = 0,
    val label: String = "",
    val scopes: List<String> = emptyList(),
    val key: String = "",
    val note: String = "",
    @SerialName("mcp_groups") val mcpGroups: List<String> = emptyList(),
)

@Serializable
internal data class ApiKeyGroupsChange(
    @SerialName("mcp_groups") val mcpGroups: List<String>,
)

@Serializable
data class ApiKeyGroups(
    val id: Long = 0,
    val label: String = "",
    @SerialName("mcp_groups") val mcpGroups: List<String> = emptyList(),
)

@Serializable
internal data class ImportPreviewBody(
    val name: String? = null,
    val password: String? = null,
)

@Serializable
data class ImportPreview(
    val people: List<ImportPerson> = emptyList(),
)

@Serializable
data class ImportPerson(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val kid: Boolean = false,
    @SerialName("server_admin") val serverAdmin: Boolean = false,
    @SerialName("linked_to") val linkedTo: String = "",
)

@Serializable
internal data class ImportPick(
    val id: String,
    val kid: Boolean? = null,
    @SerialName("confirm_not_kid") val confirmNotKid: Boolean? = null,
)

@Serializable
internal data class ImportBody(
    val people: List<ImportPick>,
)

@Serializable
data class ImportMade(
    val made: List<String> = emptyList(),
    val skipped: Int = 0,
)

@Serializable
internal data class MediaServerProbe(
    val url: String,
)

@Serializable
data class MediaServerProbed(
    val ok: Boolean = false,
    val name: String = "",
    val version: String = "",
    val product: String = "",
    val message: String = "",
)
