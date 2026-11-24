package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OutsideList(
    val id: Long = 0,
    val name: String = "",
    val kind: String = "",
    @SerialName("title_kind") val titleKind: String = "",
    val ref: String = "",
    @SerialName("library_folder_id") val libraryFolderId: Long = 0,
    @SerialName("pick_rules_id") val pickRulesId: Long? = null,
    @SerialName("keep_up_to_date") val keepUpToDate: Boolean = false,
    @SerialName("search_on_add") val searchOnAdd: Boolean = false,
    @SerialName("release_gate") val releaseGate: String = "",
    val seasons: String = "",
    @SerialName("new_seasons") val newSeasons: String = "",
    val enabled: Boolean = false,
    @SerialName("sync_every_hours") val syncEveryHours: Int = 0,
    @SerialName("last_synced_at") val lastSyncedAt: String = "",
    @SerialName("last_result") val lastResult: String = "",
    @SerialName("last_error") val lastError: String = "",
    @SerialName("job_id") val jobId: Long = 0,
)

@Serializable
data class OutsideLists(
    @SerialName("outside_lists") val outsideLists: List<OutsideList> = emptyList(),
)

@Serializable
data class OutsideListDraft(
    val name: String = "",
    val kind: String = "tmdb_list",
    @SerialName("title_kind") val titleKind: String = "movie",
    val ref: String = "",
    @SerialName("library_folder_id") val libraryFolderId: Long = 0,
    @SerialName("pick_rules_id") val pickRulesId: Long = 0,
    @SerialName("keep_up_to_date") val keepUpToDate: Boolean = true,
    @SerialName("search_on_add") val searchOnAdd: Boolean = false,
    val seasons: String = "all",
    @SerialName("new_seasons") val newSeasons: String = "all",
    val enabled: Boolean = true,
    @SerialName("sync_every_hours") val syncEveryHours: Int = 12,
)

@Serializable
data class JobQueued(
    @SerialName("job_id") val jobId: Long = 0,
)

@Serializable
internal data class KeyBody(
    val key: String,
)

@Serializable
data class MaskedKey(
    val key: String = "",
)

@Serializable
internal data class TokenBody(
    val token: String,
)

@Serializable
data class MaskedToken(
    val token: String = "",
)

@Serializable
internal data class SecretBody(
    val secret: String,
)

@Serializable
data class TraktStatus(
    @SerialName("client_id") val clientId: Boolean = false,
    @SerialName("client_secret") val clientSecret: Boolean = false,
    @SerialName("signed_in") val signedIn: Boolean = false,
    val username: String = "",
    @SerialName("person_id") val personId: Long = 0,
    @SerialName("expires_at") val expiresAt: String = "",
    val waiting: Boolean = false,
    val problem: String = "",
)

@Serializable
data class TraktDeviceCode(
    @SerialName("user_code") val userCode: String = "",
    @SerialName("verification_url") val verificationUrl: String = "",
    @SerialName("expires_in") val expiresIn: Int = 0,
    val interval: Int = 0,
)

@Serializable
data class TraktCheck(
    val state: String = "",
    val status: TraktStatus = TraktStatus(),
)

@Serializable
data class NeverAddEntry(
    val id: Long = 0,
    val kind: String = "",
    val ids: Map<String, String> = emptyMap(),
    val name: String = "",
    val reason: String = "",
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class NeverAddList(
    @SerialName("never_add") val neverAdd: List<NeverAddEntry> = emptyList(),
)

@Serializable
internal data class NewNeverAdd(
    val kind: String,
    @SerialName("tmdb_id") val tmdbId: String,
    val name: String,
    val reason: String? = null,
)
