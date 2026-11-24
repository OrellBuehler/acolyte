package ch.orellbuehler.acolyte.feature.settings.server

import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.model.SettingsServerAlert
import ch.orellbuehler.acolyte.api.model.SettingsServerAlertInput

enum class SettingsServerAlertKind(
    val wire: String,
) {
    DISCORD("discord"),
    TELEGRAM("telegram"),
    NTFY("ntfy"),
    GOTIFY("gotify"),
    PUSHOVER("pushover"),
    EMAIL("email"),
    WEBHOOK("webhook"),
    ;

    internal companion object {
        fun of(wire: String): SettingsServerAlertKind? = entries.firstOrNull { it.wire == wire }
    }
}

enum class SettingsServerMissing {
    NOTHING,
    BOT,
    BOT_AND_CHAT,
    CHAT,
    TOPIC,
    APP,
    SERVER,
    SERVER_AND_APP,
    USER_AND_APP,
    USER,
    LINK,
    ADDRESS,
}

enum class SettingsServerAlertField {
    ALERT_NAME,
    URL,
    BOT_TOKEN,
    CHAT_ID,
    SERVER_URL,
    TOPIC,
    USER_KEY,
    APP_TOKEN,
    SECRET,
    WEBHOOK_AUTH,
    SMTP_HOST,
    SMTP_PORT,
    SMTP_SECURITY,
    SMTP_FROM,
    SMTP_USER,
    SMTP_PASS,
    STAFF_TO,
}

internal object SettingsServerMoments {
    val requests: List<String> =
        listOf("ask_waiting", "ask_approved", "ask_turned_down", "ask_ready", "new_episodes", "new_albums", "report_made", "report_fixed")
    val library: List<String> =
        listOf(
            "grab",
            "downloaded",
            "download_failed",
            "import",
            "rename",
            "add",
            "delete",
            "health",
            "needs_decision",
            "plugin_message",
        )
    val all: List<String> = requests + library
}

internal fun missingFor(alert: SettingsServerAlert): SettingsServerMissing =
    when (alert.kind) {
        "telegram" ->
            when {
                alert.botToken.isNotEmpty() && alert.chatId.isNotEmpty() -> SettingsServerMissing.NOTHING
                alert.botToken.isNotEmpty() -> SettingsServerMissing.CHAT
                alert.chatId.isNotEmpty() -> SettingsServerMissing.BOT
                else -> SettingsServerMissing.BOT_AND_CHAT
            }
        "ntfy" -> if (alert.topic.isNotEmpty()) SettingsServerMissing.NOTHING else SettingsServerMissing.TOPIC
        "gotify" ->
            when {
                alert.serverUrl.isNotEmpty() && alert.appToken.isNotEmpty() -> SettingsServerMissing.NOTHING
                alert.serverUrl.isNotEmpty() -> SettingsServerMissing.APP
                alert.appToken.isNotEmpty() -> SettingsServerMissing.SERVER
                else -> SettingsServerMissing.SERVER_AND_APP
            }
        "pushover" ->
            when {
                alert.appToken.isNotEmpty() && alert.userKey.isNotEmpty() -> SettingsServerMissing.NOTHING
                alert.appToken.isNotEmpty() -> SettingsServerMissing.USER
                alert.userKey.isNotEmpty() -> SettingsServerMissing.APP
                else -> SettingsServerMissing.USER_AND_APP
            }
        "discord" -> if (alert.url.isNotEmpty()) SettingsServerMissing.NOTHING else SettingsServerMissing.LINK
        "webhook" -> if (alert.url.isNotEmpty()) SettingsServerMissing.NOTHING else SettingsServerMissing.ADDRESS
        else -> SettingsServerMissing.NOTHING
    }

data class SettingsServerMomentChoice(
    val id: String,
    val on: Boolean,
)

data class SettingsServerMomentGroup(
    val key: String,
    val moments: List<SettingsServerMomentChoice>,
) {
    val allOn: Boolean get() = moments.all { it.on }
}

const val SETTINGS_SERVER_NAME_MAX = 80
const val SETTINGS_SERVER_TOPIC_MAX = 64
const val SETTINGS_SERVER_DEFAULT_PORT = 587

data class SettingsServerAlertForm(
    val id: Long = 0,
    val kind: SettingsServerAlertKind = SettingsServerAlertKind.DISCORD,
    val name: String = "",
    val enabled: Boolean = true,
    val allowLocalNetwork: Boolean = false,
    val url: String = "",
    val botToken: String = "",
    val chatId: String = "",
    val serverUrl: String = "",
    val topic: String = "",
    val userKey: String = "",
    val appToken: String = "",
    val secret: String = "",
    val webhookAuth: String = "sign",
    val smtpHost: String = "",
    val smtpPort: String = SETTINGS_SERVER_DEFAULT_PORT.toString(),
    val smtpSecurity: String = "starttls",
    val smtpFrom: String = "",
    val smtpUser: String = "",
    val smtpPass: String = "",
    val staffTo: String = "",
    val clearAppToken: Boolean = false,
    val urlStored: Boolean = false,
    val botTokenStored: Boolean = false,
    val appTokenStored: Boolean = false,
    val secretStored: Boolean = false,
    val passStored: Boolean = false,
    val picked: List<String> = SettingsServerMoments.all,
    val saving: Boolean = false,
    val error: ApiError? = null,
) {
    val isNew: Boolean get() = id == 0L

    val groups: List<SettingsServerMomentGroup>
        get() =
            listOf(
                SettingsServerMomentGroup("requests", SettingsServerMoments.requests.map { SettingsServerMomentChoice(it, it in picked) }),
                SettingsServerMomentGroup("library", SettingsServerMoments.library.map { SettingsServerMomentChoice(it, it in picked) }),
            )

    val nameValid: Boolean get() = name.trim().let { it.isNotEmpty() && it.length <= SETTINGS_SERVER_NAME_MAX }

    val port: Int? get() = smtpPort.trim().toIntOrNull()?.takeIf { it in 1..65535 }

    val portValid: Boolean get() = kind != SettingsServerAlertKind.EMAIL || port != null

    val requiredFilled: Boolean
        get() =
            when (kind) {
                SettingsServerAlertKind.DISCORD, SettingsServerAlertKind.WEBHOOK -> url.isNotBlank()
                SettingsServerAlertKind.TELEGRAM -> botToken.isNotBlank() && chatId.isNotBlank()
                SettingsServerAlertKind.NTFY -> topic.isNotBlank()
                SettingsServerAlertKind.GOTIFY -> serverUrl.isNotBlank() && appToken.isNotBlank()
                SettingsServerAlertKind.PUSHOVER -> userKey.isNotBlank() && appToken.isNotBlank()
                SettingsServerAlertKind.EMAIL -> smtpHost.isNotBlank() && smtpFrom.isNotBlank()
            }

    val canSave: Boolean get() = !saving && nameValid && portValid && (!isNew || requiredFilled)

    val canClearToken: Boolean get() = kind == SettingsServerAlertKind.NTFY && !isNew && appTokenStored

    fun text(field: SettingsServerAlertField): String =
        when (field) {
            SettingsServerAlertField.ALERT_NAME -> name
            SettingsServerAlertField.URL -> url
            SettingsServerAlertField.BOT_TOKEN -> botToken
            SettingsServerAlertField.CHAT_ID -> chatId
            SettingsServerAlertField.SERVER_URL -> serverUrl
            SettingsServerAlertField.TOPIC -> topic
            SettingsServerAlertField.USER_KEY -> userKey
            SettingsServerAlertField.APP_TOKEN -> appToken
            SettingsServerAlertField.SECRET -> secret
            SettingsServerAlertField.WEBHOOK_AUTH -> webhookAuth
            SettingsServerAlertField.SMTP_HOST -> smtpHost
            SettingsServerAlertField.SMTP_PORT -> smtpPort
            SettingsServerAlertField.SMTP_SECURITY -> smtpSecurity
            SettingsServerAlertField.SMTP_FROM -> smtpFrom
            SettingsServerAlertField.SMTP_USER -> smtpUser
            SettingsServerAlertField.SMTP_PASS -> smtpPass
            SettingsServerAlertField.STAFF_TO -> staffTo
        }

    internal fun withText(
        field: SettingsServerAlertField,
        value: String,
    ): SettingsServerAlertForm =
        when (field) {
            SettingsServerAlertField.ALERT_NAME -> copy(name = value.take(SETTINGS_SERVER_NAME_MAX))
            SettingsServerAlertField.URL -> copy(url = value)
            SettingsServerAlertField.BOT_TOKEN -> copy(botToken = value)
            SettingsServerAlertField.CHAT_ID -> copy(chatId = value)
            SettingsServerAlertField.SERVER_URL -> copy(serverUrl = value)
            SettingsServerAlertField.TOPIC -> copy(topic = value.filter { it.isTopicChar() }.take(SETTINGS_SERVER_TOPIC_MAX))
            SettingsServerAlertField.USER_KEY -> copy(userKey = value)
            SettingsServerAlertField.APP_TOKEN -> copy(appToken = value, clearAppToken = if (value.isEmpty()) clearAppToken else false)
            SettingsServerAlertField.SECRET -> copy(secret = value)
            SettingsServerAlertField.WEBHOOK_AUTH -> copy(webhookAuth = value)
            SettingsServerAlertField.SMTP_HOST -> copy(smtpHost = value)
            SettingsServerAlertField.SMTP_PORT -> copy(smtpPort = value.filter { it.isDigit() }.take(5))
            SettingsServerAlertField.SMTP_SECURITY -> copy(smtpSecurity = value)
            SettingsServerAlertField.SMTP_FROM -> copy(smtpFrom = value)
            SettingsServerAlertField.SMTP_USER -> copy(smtpUser = value)
            SettingsServerAlertField.SMTP_PASS -> copy(smtpPass = value)
            SettingsServerAlertField.STAFF_TO -> copy(staffTo = value)
        }

    internal fun withKind(next: SettingsServerAlertKind): SettingsServerAlertForm =
        if (next == kind) this else copy(kind = next, appToken = "", serverUrl = "", clearAppToken = false)

    internal fun withClearToken(on: Boolean): SettingsServerAlertForm = copy(clearAppToken = on, appToken = if (on) "" else appToken)

    internal fun withMoment(
        id: String,
        on: Boolean,
    ): SettingsServerAlertForm = copy(picked = if (on) (picked + id).distinct() else picked - id)

    internal fun withGroup(
        key: String,
        on: Boolean,
    ): SettingsServerAlertForm {
        val ids = if (key == "requests") SettingsServerMoments.requests else SettingsServerMoments.library
        return copy(picked = if (on) (picked + ids).distinct() else picked.filter { it !in ids })
    }

    internal fun toInput(): SettingsServerAlertInput {
        val base =
            SettingsServerAlertInput(
                name = name.trim(),
                kind = kind.wire,
                enabled = enabled,
                moments = picked,
                allowLocalNetwork = allowLocalNetwork,
            )
        return when (kind) {
            SettingsServerAlertKind.DISCORD -> base.copy(url = url.trim())
            SettingsServerAlertKind.TELEGRAM -> base.copy(botToken = botToken.trim(), chatId = chatId.trim())
            SettingsServerAlertKind.NTFY ->
                base.copy(serverUrl = serverUrl.trim(), topic = topic.trim(), appToken = appToken.trim(), clearAppToken = clearAppToken)
            SettingsServerAlertKind.GOTIFY -> base.copy(serverUrl = serverUrl.trim(), appToken = appToken.trim())
            SettingsServerAlertKind.PUSHOVER -> base.copy(userKey = userKey.trim(), appToken = appToken.trim())
            SettingsServerAlertKind.EMAIL ->
                base.copy(
                    smtpHost = smtpHost.trim(),
                    smtpPort = port ?: SETTINGS_SERVER_DEFAULT_PORT,
                    smtpSecurity = smtpSecurity,
                    smtpFrom = smtpFrom.trim(),
                    smtpUser = smtpUser.trim(),
                    smtpPass = smtpPass,
                    staffTo = staffTo.trim(),
                )
            SettingsServerAlertKind.WEBHOOK -> base.copy(url = url.trim(), secret = secret, webhookAuth = webhookAuth)
        }
    }

    internal companion object {
        fun blank(): SettingsServerAlertForm = SettingsServerAlertForm()

        fun from(alert: SettingsServerAlert): SettingsServerAlertForm =
            SettingsServerAlertForm(
                id = alert.id,
                kind = SettingsServerAlertKind.of(alert.kind) ?: SettingsServerAlertKind.WEBHOOK,
                name = alert.name,
                enabled = alert.enabled,
                allowLocalNetwork = alert.allowLocalNetwork,
                chatId = alert.chatId,
                serverUrl = alert.serverUrl,
                topic = alert.topic,
                userKey = alert.userKey,
                webhookAuth = alert.webhookAuth.ifEmpty { "sign" },
                smtpHost = alert.smtpHost,
                smtpPort = (alert.smtpPort.takeIf { it > 0 } ?: SETTINGS_SERVER_DEFAULT_PORT).toString(),
                smtpSecurity = alert.smtpSecurity.ifEmpty { "starttls" },
                smtpFrom = alert.smtpFrom,
                smtpUser = alert.smtpUser,
                staffTo = alert.staffTo,
                urlStored = alert.url.isNotEmpty(),
                botTokenStored = alert.botToken.isNotEmpty(),
                appTokenStored = alert.appToken.isNotEmpty(),
                secretStored = alert.secret.isNotEmpty(),
                passStored = alert.smtpPass.isNotEmpty(),
                picked = alert.moments,
            )
    }
}

private fun Char.isTopicChar(): Boolean = this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9' || this == '-' || this == '_'
