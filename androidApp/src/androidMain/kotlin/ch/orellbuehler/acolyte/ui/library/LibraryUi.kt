package ch.orellbuehler.acolyte.ui.library

import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.feature.library.LibraryKind
import ch.orellbuehler.acolyte.feature.library.LibraryNotice
import ch.orellbuehler.acolyte.feature.library.LibraryNoticeKind
import ch.orellbuehler.acolyte.image.ImageSize
import ch.orellbuehler.acolyte.ui.components.ServerImage
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.theme.Spacing
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

class PresenterViewModel<P>(
    val presenter: P,
    start: suspend P.() -> Unit,
) : ViewModel() {
    private val latest = mutableMapOf<String, Job>()

    init {
        viewModelScope.launch { presenter.start() }
    }

    fun run(block: suspend P.() -> Unit) {
        viewModelScope.launch { presenter.block() }
    }

    fun latest(
        key: String,
        block: suspend P.() -> Unit,
    ) {
        latest[key]?.cancel()
        latest[key] = viewModelScope.launch { presenter.block() }
    }
}

@Composable
fun WhileVisible(block: suspend () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { block() } }
}

object LibraryFormat {
    private val units = listOf("B", "KB", "MB", "GB", "TB")

    fun bytes(value: Long): String {
        var size = value.coerceAtLeast(0).toDouble()
        var unit = 0
        while (size >= 1024 && unit < units.lastIndex) {
            size /= 1024
            unit++
        }
        val text =
            if (unit > 0 &&
                size < 10
            ) {
                String.format(Locale.getDefault(), "%.1f", size)
            } else {
                String.format(Locale.getDefault(), "%.0f", size)
            }
        return "$text ${units[unit]}"
    }

    private fun instant(iso: String): Instant? =
        if (iso.isBlank()) {
            null
        } else {
            try {
                Instant.parse(iso)
            } catch (e: Exception) {
                null
            }
        }

    fun day(iso: String): String {
        val instant = instant(iso) ?: return ""
        val locale = Locale.getDefault()
        val pattern = DateFormat.getBestDateTimePattern(locale, "dMMMyyyy")
        return DateTimeFormatter.ofPattern(pattern, locale).withZone(ZoneOffset.UTC).format(instant)
    }

    fun dateTime(iso: String): String {
        val instant = instant(iso) ?: return ""
        val locale = Locale.getDefault()
        val pattern = DateFormat.getBestDateTimePattern(locale, "dMMMHHmm")
        return DateTimeFormatter.ofPattern(pattern, locale).withZone(ZoneId.systemDefault()).format(instant)
    }

    fun ago(iso: String): String {
        val instant = instant(iso) ?: return ""
        return DateUtils
            .getRelativeTimeSpanString(
                instant.toEpochMilli(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
            ).toString()
    }

    fun codec(id: String): String =
        when (id) {
            "h264" -> "H.264"
            "hevc" -> "HEVC"
            "av1" -> "AV1"
            "vp9" -> "VP9"
            "mpeg2" -> "MPEG-2"
            "xvid" -> "Xvid"
            "vc1" -> "VC-1"
            else -> id.uppercase()
        }
}

@Composable
fun codecLabel(id: String): String = if (id == "unknown") stringResource(R.string.library_codec_unknown) else LibraryFormat.codec(id)

@Composable
fun kindLabel(kind: LibraryKind): String =
    stringResource(
        when (kind) {
            LibraryKind.MOVIE -> R.string.library_kind_movie
            LibraryKind.SHOW -> R.string.library_kind_show
            LibraryKind.ARTIST -> R.string.library_kind_music
        },
    )

@Composable
fun libraryErrorText(error: ApiError): String {
    val base = errorText(error)
    return if ((error.code == ApiErrorCode.BAD_REQUEST || error.code == ApiErrorCode.CONFLICT) && error.message.isNotBlank()) {
        base + "\n" + stringResource(R.string.library_server_says, error.message)
    } else {
        base
    }
}

@Composable
fun noticeText(notice: LibraryNotice): String {
    val n = notice.count
    if (notice.kind == LibraryNoticeKind.FAILED) {
        if (notice.name == "missing") return stringResource(R.string.library_notice_folder_missing)
        return notice.error?.let { libraryErrorText(it) } ?: stringResource(R.string.error_unknown)
    }
    if (notice.partly && notice.total > 0) {
        val text = stringResource(R.string.library_notice_partly, n, notice.total, notice.total - n)
        return notice.error?.let { text + "\n" + libraryErrorText(it) } ?: text
    }
    return when (notice.kind) {
        LibraryNoticeKind.KEPT -> pluralStringResource(R.plurals.library_notice_kept, n, n)
        LibraryNoticeKind.UNKEPT -> pluralStringResource(R.plurals.library_notice_unkept, n, n)
        LibraryNoticeKind.RULES_SET -> pluralStringResource(R.plurals.library_notice_rules, n, n, notice.name)
        LibraryNoticeKind.AUDIO_SET -> pluralStringResource(R.plurals.library_notice_audio, n, n)
        LibraryNoticeKind.SEARCHING -> pluralStringResource(R.plurals.library_notice_searching, n, n)
        LibraryNoticeKind.REFRESHING -> pluralStringResource(R.plurals.library_notice_refreshing, n, n)
        LibraryNoticeKind.MOVING -> pluralStringResource(R.plurals.library_notice_moving, n, n, notice.name)
        LibraryNoticeKind.REMOVED -> pluralStringResource(R.plurals.library_notice_removed, n, n)
        LibraryNoticeKind.COPIES_SEARCHING -> pluralStringResource(R.plurals.library_notice_copies, n, n)
        LibraryNoticeKind.MISSING_SEARCHING -> pluralStringResource(R.plurals.library_notice_missing, n, n)
        LibraryNoticeKind.SUBTITLES_SEARCHING -> pluralStringResource(R.plurals.library_notice_subtitles, n, n)
        LibraryNoticeKind.FILTER_SAVED -> stringResource(R.string.library_notice_filter_saved, notice.name)
        LibraryNoticeKind.RESTORED -> pluralStringResource(R.plurals.library_notice_restored, n, n)
        LibraryNoticeKind.SWAPPED -> stringResource(R.string.library_notice_swapped, notice.name)
        LibraryNoticeKind.RESTORE_FAILED -> {
            val base = stringResource(R.string.library_notice_restore_failed, notice.name)
            notice.error?.let { base + "\n" + it.message } ?: base
        }
        LibraryNoticeKind.TAKEN_OFF -> stringResource(R.string.library_notice_taken_off)
        LibraryNoticeKind.CLEARED -> pluralStringResource(R.plurals.library_notice_cleared, n, n)
        LibraryNoticeKind.RENAMED -> pluralStringResource(R.plurals.library_notice_renamed, n, n)
        LibraryNoticeKind.RENAME_CHANGED -> stringResource(R.string.library_rename_changed)
        LibraryNoticeKind.ADOPTED -> pluralStringResource(R.plurals.library_notice_adopted, n, n)
        LibraryNoticeKind.TIDIED -> pluralStringResource(R.plurals.library_notice_tidied, n, n)
        LibraryNoticeKind.UNTIDIED -> pluralStringResource(R.plurals.library_notice_untidied, n, n)
        LibraryNoticeKind.FLAT_ON -> stringResource(R.string.library_notice_flat)
        LibraryNoticeKind.IMPORTED -> pluralStringResource(R.plurals.library_notice_imported, n, n)
        LibraryNoticeKind.FOLDER_ADDED -> stringResource(R.string.library_notice_folder_added)
        LibraryNoticeKind.FOLDER_CHECKED -> stringResource(R.string.library_notice_folder_checked)
        LibraryNoticeKind.FOLDER_REMOVED -> stringResource(R.string.library_notice_folder_removed)
        LibraryNoticeKind.PATH_CHANGED -> stringResource(R.string.library_notice_path_changed)
        LibraryNoticeKind.IMPORT_FOLDERS_SAVED -> stringResource(R.string.library_notice_import_folders)
        LibraryNoticeKind.SIDE_FILES_SAVED -> stringResource(R.string.library_notice_side_files)
        LibraryNoticeKind.HANDS_OFF_ON -> stringResource(R.string.library_notice_hands_off_on)
        LibraryNoticeKind.HANDS_OFF_OFF -> stringResource(R.string.library_notice_hands_off_off)
        LibraryNoticeKind.TITLE_DELETED -> stringResource(R.string.library_notice_title_deleted, notice.name)
        LibraryNoticeKind.FAILED -> stringResource(R.string.error_unknown)
    }
}

@Composable
fun BoxScope.NoticeHost(
    notice: LibraryNotice?,
    onDismiss: (Int) -> Unit,
) {
    val host = remember { SnackbarHostState() }
    val text = notice?.let { noticeText(it) }
    LaunchedEffect(notice?.seq) {
        if (notice != null && text != null) {
            host.showSnackbar(text, withDismissAction = true, duration = SnackbarDuration.Long)
            onDismiss(notice.seq)
        }
    }
    SnackbarHost(host, Modifier.align(Alignment.BottomCenter).padding(Spacing.Small))
}

@Composable
fun PosterThumb(
    source: String?,
    name: String,
    modifier: Modifier = Modifier,
    width: Dp = 40.dp,
) {
    ServerImage(
        source = source,
        contentDescription = null,
        size = ImageSize.THUMB,
        placeholderName = name,
        modifier =
            modifier
                .width(width)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(6.dp)),
    )
}

@Composable
fun Pager(
    from: Int,
    to: Int,
    total: Int,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrevious, enabled = hasPrevious) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = stringResource(R.string.library_previous))
        }
        Text(
            stringResource(R.string.library_page, from, to, total),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(onClick = onNext, enabled = hasNext) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = stringResource(R.string.library_next))
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String?,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = text?.let { { Text(it) } },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onConfirm()
            }) {
                Text(confirm, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.library_cancel)) } },
    )
}

@Composable
fun TextInputDialog(
    title: String,
    label: String,
    confirm: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    initial: String = "",
    hint: String? = null,
    maxLength: Int = 256,
    allowEmpty: Boolean = false,
) {
    var value by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.take(maxLength) },
                    label = { Text(label) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(
                enabled = allowEmpty || value.isNotBlank(),
                onClick = {
                    onDismiss()
                    onConfirm(value)
                },
            ) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.library_cancel)) } },
    )
}

@Composable
fun MutedText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

@Composable
fun Centered(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxWidth().padding(Spacing.Large), contentAlignment = Alignment.Center, content = content)
}

@Composable
fun rememberContextLocale(): Locale = LocalContext.current.resources.configuration.locales[0]
