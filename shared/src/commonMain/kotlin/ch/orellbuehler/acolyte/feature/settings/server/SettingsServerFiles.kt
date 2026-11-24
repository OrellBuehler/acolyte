package ch.orellbuehler.acolyte.feature.settings.server

import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.TransferredFile
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

enum class SettingsServerFileKind {
    BACKUP,
    KEY_KIT,
    LOG,
}

data class SettingsServerFile(
    val seq: Int,
    val kind: SettingsServerFileKind,
    val path: String,
    val name: String,
    val bytes: Long,
) {
    fun toTransferred(): TransferredFile = TransferredFile(path, name, bytes)
}

val ApiError.showsServerSentence: Boolean
    get() = message.isNotBlank() && status in SENTENCE_STATUSES

private val SENTENCE_STATUSES = setOf(400, 409, 422, 500, 502, 503, 507)

internal fun deleteTransfer(path: String) {
    runCatching { SystemFileSystem.delete(Path(path)) }
}
