package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class AuthState(
    @SerialName("needs_setup") val needsSetup: Boolean = false,
    val ways: List<String> = emptyList(),
    @SerialName("safe_mode") val safeMode: Boolean = false,
    @SerialName("min_password_length") val minPasswordLength: Int = 8,
    @SerialName("host_refused") val hostRefused: String? = null,
    @SerialName("setup_open") val setupOpen: Boolean = false,
    @SerialName("setup_ways") val setupWays: List<String> = emptyList(),
    @SerialName("config_unmapped") val configUnmapped: Boolean = false,
    @SerialName("owner_reset") val ownerReset: Boolean = false,
    @SerialName("owner_reset_env") val ownerResetEnv: Boolean = false,
)

@Serializable
internal data class PasswordSignIn(
    val name: String,
    val password: String,
)

@Serializable
internal data class TwoFactorSignIn(
    val ticket: String,
    val code: String,
)

@Serializable
internal data class SignInAnswer(
    val name: String = "",
    val csrf: String = "",
    @SerialName("two_factor") val twoFactor: Boolean = false,
    val ticket: String = "",
    val waiting: Boolean = false,
)

@Serializable
data class Me(
    val id: Long = 0,
    val name: String = "",
    val via: String = "",
    val csrf: String = "",
    val role: String = "",
    val perms: List<String> = emptyList(),
    @SerialName("two_factor") val twoFactor: Boolean = false,
    @SerialName("must_two_factor") val mustTwoFactor: Boolean = false,
    @SerialName("auth_method") val authMethod: String = "",
    @SerialName("has_password") val hasPassword: Boolean = true,
    val avatar: String? = null,
    @SerialName("safe_mode") val safeMode: Boolean = false,
    val held: List<String> = emptyList(),
    @SerialName("max_rating") val maxRating: String = "",
    @SerialName("rating_country") val ratingCountry: String = "",
    @SerialName("hands_off") val handsOff: Boolean = false,
    @SerialName("setup_open") val setupOpen: Boolean = false,
    val views: JsonObject = JsonObject(emptyMap()),
    @SerialName("setup_skipped") val setupSkipped: Boolean = false,
)

@Serializable
internal data class Reauth(
    val password: String,
    val code: String? = null,
)
