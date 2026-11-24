package ch.orellbuehler.acolyte.api.model

import ch.orellbuehler.acolyte.api.RawBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AccountNameChange(
    @SerialName("display_name") val displayName: String,
)

@Serializable
internal data class AccountLanguageChange(
    val language: String,
)

@Serializable
data class AccountAvatar(
    val avatar: String? = null,
)

@Serializable
data class AccountAlerts(
    val email: String = "",
    @SerialName("discord_id") val discordId: String = "",
    @SerialName("new_episodes") val newEpisodes: Boolean? = null,
)

@Serializable
data class AccountFollow(
    @SerialName("title_id") val titleId: Long = 0,
    val kind: String = "",
    val name: String = "",
    val year: Int = 0,
    @SerialName("followed_at") val followedAt: String = "",
    @SerialName("tmdb_id") val tmdbId: String = "",
    val mbid: String = "",
    val artist: String = "",
    val unfollow: String = "",
)

@Serializable
data class AccountFollows(
    val follows: List<AccountFollow> = emptyList(),
)

@Serializable
internal data class AccountPasswordChange(
    val current: String? = null,
    @SerialName("new") val newPassword: String,
)

@Serializable
data class AccountTwoFactorSetup(
    val secret: String = "",
    val uri: String = "",
    val qr: List<String> = emptyList(),
)

@Serializable
internal data class AccountCode(
    val code: String,
)

@Serializable
data class AccountBackupCodes(
    @SerialName("backup_codes") val backupCodes: List<String> = emptyList(),
)

@Serializable
data class AccountSession(
    val id: String = "",
    val device: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("last_used_at") val lastUsedAt: String = "",
    @SerialName("expires_at") val expiresAt: String = "",
    val current: Boolean = false,
)

@Serializable
data class AccountSessions(
    val sessions: List<AccountSession> = emptyList(),
)

@Serializable
data class AccountEnded(
    val ended: Int = 0,
)

@Serializable
data class AccountSsoLink(
    val kind: String = "",
    val issuer: String = "",
    val name: String = "",
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class AccountSsoLinks(
    val links: List<AccountSsoLink> = emptyList(),
)

@Serializable
data class AccountPlexLink(
    val linked: Boolean = false,
    val name: String = "",
    val kept: Boolean = false,
    val waiting: Boolean = false,
)

@Serializable
data class AccountMediaServer(
    val id: Long = 0,
    val name: String = "",
    val kind: String = "",
)

@Serializable
data class AccountMediaLink(
    val provider: String = "",
    val name: String = "",
)

@Serializable
data class AccountMediaLinks(
    val servers: List<AccountMediaServer> = emptyList(),
    val links: List<AccountMediaLink> = emptyList(),
)

@Serializable
internal data class AccountMediaLinkBody(
    val name: String,
    val password: String,
)

@Serializable
data class AccountKey(
    val id: Long = 0,
    val label: String = "",
    val hint: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("last_used_at") val lastUsedAt: String = "",
    val scopes: List<String> = emptyList(),
    @SerialName("mcp_groups") val mcpGroups: List<String> = emptyList(),
)

@Serializable
data class AccountKeys(
    val keys: List<AccountKey> = emptyList(),
)

@Serializable
internal data class AccountKeyMake(
    val label: String,
    val scopes: List<String>? = null,
    @SerialName("mcp_groups") val mcpGroups: List<String>? = null,
)

@Serializable
data class AccountKeyMade(
    val id: Long = 0,
    val label: String = "",
    val scopes: List<String> = emptyList(),
    val key: String = "",
    val note: String = "",
    @SerialName("mcp_groups") val mcpGroups: List<String> = emptyList(),
)

@Serializable
internal data class AccountKeyChange(
    @SerialName("mcp_groups") val mcpGroups: List<String>,
)

@Serializable
data class AccountKeyChanged(
    val id: Long = 0,
    val label: String = "",
    @SerialName("mcp_groups") val mcpGroups: List<String> = emptyList(),
)

@Serializable
internal data class AccountAddressTest(
    val url: String,
)

@Serializable
data class AccountAddressAnswer(
    val ok: Boolean = false,
    val message: String = "",
    val name: String = "",
    val version: String = "",
    val product: String = "",
)

@Serializable
data class AccountImportPerson(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val kid: Boolean = false,
    @SerialName("server_admin") val serverAdmin: Boolean = false,
    @SerialName("linked_to") val linkedTo: String = "",
)

@Serializable
data class AccountImportPeople(
    val people: List<AccountImportPerson> = emptyList(),
)

@Serializable
internal data class AccountImportPreview(
    val name: String? = null,
    val password: String? = null,
)

@Serializable
internal data class AccountImportPick(
    val id: String,
    val kid: Boolean? = null,
    @SerialName("confirm_not_kid") val confirmNotKid: Boolean? = null,
)

@Serializable
internal data class AccountImportBody(
    val people: List<AccountImportPick>,
)

@Serializable
data class AccountImported(
    val made: List<String> = emptyList(),
    val skipped: Int = 0,
)

@Serializable
data class AccountAiInfo(
    @SerialName("client_name") val clientName: String = "",
    @SerialName("returns_to") val returnsTo: String = "",
    val local: Boolean = false,
    @SerialName("name_verified") val nameVerified: Boolean = false,
    val enabled: Boolean = false,
    @SerialName("deny_url") val denyUrl: String = "",
)

@Serializable
internal data class AccountAiAllow(
    val query: String,
)

@Serializable
data class AccountAiAllowed(
    val redirect: String = "",
)

@Serializable
internal class AccountPicture(
    override val rawBytes: ByteArray,
    override val rawType: String,
) : RawBody
