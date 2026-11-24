package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PeopleLink(
    val provider: String = "",
    val name: String = "",
)

@Serializable
data class PeopleInvite(
    val id: String = "",
    val role: String = "",
    val perms: List<String> = emptyList(),
    val note: String = "",
    @SerialName("created_by") val createdBy: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("expires_at") val expiresAt: String = "",
    @SerialName("used_at") val usedAt: String = "",
    @SerialName("used_by") val usedBy: String = "",
    @SerialName("max_rating") val maxRating: String = "",
    @SerialName("rating_country") val ratingCountry: String = "",
    @SerialName("max_uses") val maxUses: Int = 0,
    val uses: Int = 0,
    val joined: List<String> = emptyList(),
    @SerialName("never_expires") val neverExpires: Boolean = false,
)

@Serializable
data class PeopleInvites(
    val invites: List<PeopleInvite> = emptyList(),
)

@Serializable
internal data class PeopleInviteBody(
    val role: String,
    val perms: List<String>,
    val note: String,
    val days: Int,
    @SerialName("max_uses") val maxUses: Int,
    @SerialName("max_rating") val maxRating: String,
    @SerialName("rating_country") val ratingCountry: String,
)

@Serializable
data class PeopleInviteMade(
    val id: String = "",
    val path: String = "",
    val role: String = "",
    val perms: List<String> = emptyList(),
    @SerialName("expires_at") val expiresAt: String = "",
    @SerialName("never_expires") val neverExpires: Boolean = false,
    @SerialName("max_uses") val maxUses: Int = 0,
    val note: String = "",
    @SerialName("max_rating") val maxRating: String = "",
    @SerialName("rating_country") val ratingCountry: String = "",
)

@Serializable
internal data class PeopleChange(
    val role: String? = null,
    val perms: List<String>? = null,
    val state: String? = null,
    @SerialName("max_rating") val maxRating: String? = null,
    @SerialName("rating_country") val ratingCountry: String? = null,
)

@Serializable
data class PeopleChanged(
    val id: Long = 0,
    val role: String = "",
    val state: String = "",
    val perms: List<String> = emptyList(),
    @SerialName("max_rating") val maxRating: String = "",
    @SerialName("rating_country") val ratingCountry: String = "",
)

@Serializable
internal data class PeoplePassword(
    val password: String,
)

@Serializable
internal data class PeopleSsoUnlink(
    val kind: String,
    val issuer: String,
    @SerialName("confirm_no_way_in") val confirmNoWayIn: Boolean? = null,
)

@Serializable
data class PeopleServerAccount(
    val id: String = "",
    val name: String = "",
    @SerialName("taken_by") val takenBy: Long = 0,
    @SerialName("taken_name") val takenName: String = "",
)

@Serializable
data class PeopleMediaServer(
    val id: Long = 0,
    val name: String = "",
    val kind: String = "",
    val accounts: List<PeopleServerAccount> = emptyList(),
    val error: String = "",
)

@Serializable
data class PeopleMediaAccounts(
    val servers: List<PeopleMediaServer> = emptyList(),
)

@Serializable
internal data class PeopleMediaLinkBody(
    val server: Long,
    val account: String,
)
