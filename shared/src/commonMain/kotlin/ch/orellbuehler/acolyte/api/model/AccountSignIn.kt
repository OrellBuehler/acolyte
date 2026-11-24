package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AccountPlexStart(
    val id: Long = 0,
    @SerialName("auth_url") val authUrl: String = "",
)

@Serializable
internal data class AccountPlexFinish(
    val id: Long,
)

@Serializable
internal data class AccountResetCode(
    val code: String,
)

@Serializable
internal data class AccountResetFinish(
    val code: String,
    val name: String,
    val password: String,
)

@Serializable
internal data class AccountResetAnswer(
    val ok: Boolean = false,
    val name: String = "",
)

@Serializable
internal class AccountEmpty

@Serializable
internal data class AccountSetupOwner(
    val name: String,
    val password: String,
    @SerialName("display_name") val displayName: String,
)

@Serializable
internal data class AccountSetupLink(
    val url: String,
    val name: String,
    val password: String,
)

@Serializable
data class AccountSetupLinked(
    val provider: String = "",
    val name: String = "",
    val url: String = "",
)

@Serializable
data class AccountFoundServer(
    val kind: String = "",
    val name: String = "",
    val address: String = "",
)

@Serializable
data class AccountFoundServers(
    val servers: List<AccountFoundServer> = emptyList(),
)

@Serializable
data class AccountSetupDone(
    val done: Boolean = false,
)

@Serializable
data class InviteLook(
    val role: String = "",
    val note: String = "",
    @SerialName("max_rating") val maxRating: String = "",
    @SerialName("rating_country") val ratingCountry: String = "",
)

@Serializable
internal data class InviteJoinBody(
    val name: String,
    val password: String,
)
