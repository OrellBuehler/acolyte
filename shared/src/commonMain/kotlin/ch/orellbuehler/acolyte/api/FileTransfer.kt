package ch.orellbuehler.acolyte.api

import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

data class TransferredFile(
    val path: String,
    val name: String,
    val bytes: Long,
)

internal expect fun transferDirectory(): String

internal fun transferPath(name: String): String {
    val directory = Path(transferDirectory(), "acolyte-transfers")
    SystemFileSystem.createDirectories(directory)
    return Path(directory, safeFileName(name)).toString()
}

internal fun safeFileName(name: String): String {
    val cleaned =
        name
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .filter { it.isLetterOrDigit() || it in "._- " }
            .trim()
            .trim('.')
    return cleaned.ifEmpty { "download" }
}

internal fun HttpResponse.attachmentName(fallback: String): String {
    val header = headers[HttpHeaders.ContentDisposition].orEmpty()
    val quoted = Regex("filename=\"([^\"]+)\"").find(header)?.groupValues?.get(1)
    val plain = Regex("filename=([^;\\s]+)").find(header)?.groupValues?.get(1)
    return safeFileName(quoted ?: plain ?: fallback)
}

fun clearTransfers() {
    val directory = Path(transferDirectory(), "acolyte-transfers")
    if (!SystemFileSystem.exists(directory)) return
    SystemFileSystem.list(directory).forEach { runCatching { SystemFileSystem.delete(it) } }
}

internal fun fileSize(path: String): Long = SystemFileSystem.metadataOrNull(Path(path))?.size ?: 0L

internal fun openSink(path: String) = SystemFileSystem.sink(Path(path)).buffered()

internal fun openSource(path: String) = SystemFileSystem.source(Path(path)).buffered()
