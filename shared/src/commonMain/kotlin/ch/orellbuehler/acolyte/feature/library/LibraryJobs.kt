package ch.orellbuehler.acolyte.feature.library

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.ApiJson
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.model.JobView
import kotlinx.coroutines.delay
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonPrimitive

enum class LibraryNoticeKind {
    KEPT,
    UNKEPT,
    RULES_SET,
    AUDIO_SET,
    SEARCHING,
    REFRESHING,
    MOVING,
    REMOVED,
    COPIES_SEARCHING,
    MISSING_SEARCHING,
    SUBTITLES_SEARCHING,
    FILTER_SAVED,
    RESTORED,
    SWAPPED,
    RESTORE_FAILED,
    TAKEN_OFF,
    CLEARED,
    RENAMED,
    RENAME_CHANGED,
    ADOPTED,
    TIDIED,
    UNTIDIED,
    FLAT_ON,
    IMPORTED,
    FOLDER_ADDED,
    FOLDER_CHECKED,
    FOLDER_REMOVED,
    PATH_CHANGED,
    IMPORT_FOLDERS_SAVED,
    SIDE_FILES_SAVED,
    HANDS_OFF_ON,
    HANDS_OFF_OFF,
    TITLE_DELETED,
    FAILED,
}

data class LibraryNotice(
    val seq: Int,
    val kind: LibraryNoticeKind,
    val count: Int = 0,
    val total: Int = 0,
    val name: String = "",
    val error: ApiError? = null,
) {
    val partly: Boolean get() = total > count
}

internal class Notices {
    private var seq = 0

    fun make(
        kind: LibraryNoticeKind,
        count: Int = 0,
        total: Int = 0,
        name: String = "",
        error: ApiError? = null,
    ): LibraryNotice = LibraryNotice(++seq, kind, count, total, name, error)

    fun failed(error: ApiError): LibraryNotice = make(LibraryNoticeKind.FAILED, error = error)
}

data class JobProgress(
    val running: Boolean = false,
    val progress: Double = 0.0,
    val note: String = "",
)

internal data class JobOutcome(
    val job: JobView?,
    val error: ApiError?,
) {
    val done: Boolean get() = job?.state == "done"
}

internal object LibraryJobs {
    private val ended = setOf("done", "failed", "cancelled")
    private val transient = setOf(ApiErrorCode.OFFLINE, ApiErrorCode.TIMEOUT, ApiErrorCode.SERVER)
    private val backoff = listOf(1_000L, 2_000L, 4_000L, 8_000L, 15_000L)
    private const val POLL_MILLIS = 700L
    private const val SILENCE_MILLIS = 10 * 60 * 1000L

    suspend fun follow(
        api: Api,
        id: Long,
        onProgress: (JobView) -> Unit,
    ): JobOutcome {
        var last: JobView? = null
        var changedAt = LibraryLogic.now()
        var failures = 0
        while (true) {
            when (val result = api.result(LibraryEndpoints.job, "id" to id)) {
                is ApiResult.Ok -> {
                    failures = 0
                    val job = result.value
                    if (last == null || job.state != last.state || job.progress != last.progress || job.progressNote != last.progressNote) {
                        changedAt = LibraryLogic.now()
                    }
                    last = job
                    onProgress(job)
                    if (job.state in ended) return JobOutcome(job, null)
                }
                is ApiResult.Failed -> {
                    if (result.error.code !in transient || failures >= backoff.size) return JobOutcome(last, result.error)
                    delay(backoff[failures++])
                    continue
                }
            }
            if (LibraryLogic.now() - changedAt > SILENCE_MILLIS) return JobOutcome(last, ApiError(ApiErrorCode.TIMEOUT))
            delay(POLL_MILLIS)
        }
    }

    fun <T> result(
        job: JobView?,
        name: String,
        serializer: KSerializer<T>,
    ): T? {
        val value = job?.result?.get(name) ?: return null
        return try {
            ApiJson.lenient.decodeFromJsonElement(serializer, value)
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun count(
        job: JobView?,
        name: String,
    ): Int = (job?.result?.get(name) as? JsonPrimitive)?.content?.toDoubleOrNull()?.toInt() ?: 0
}
