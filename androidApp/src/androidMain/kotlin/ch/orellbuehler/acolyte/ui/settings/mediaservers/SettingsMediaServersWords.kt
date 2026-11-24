package ch.orellbuehler.acolyte.ui.settings.mediaservers

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.settings.mediaservers.SettingsMediaServersNotice
import ch.orellbuehler.acolyte.feature.settings.mediaservers.SettingsMediaServersNoticeKind
import ch.orellbuehler.acolyte.feature.settings.mediaservers.SettingsMediaServersRow
import ch.orellbuehler.acolyte.feature.settings.mediaservers.SettingsMediaServersTag
import ch.orellbuehler.acolyte.ui.components.errorText
import java.util.Locale

private const val OFF = "off"

@Composable
fun mediaServersNoticeText(notice: SettingsMediaServersNotice): String {
    val head =
        when (notice.kind) {
            SettingsMediaServersNoticeKind.SAVED -> stringResource(R.string.settings_mediaservers_saved, notice.name)
            SettingsMediaServersNoticeKind.REMOVE_FAILED -> stringResource(R.string.settings_mediaservers_remove_failed)
            SettingsMediaServersNoticeKind.WATCH_ON -> stringResource(R.string.settings_mediaservers_watch_on_done)
            SettingsMediaServersNoticeKind.WATCH_OFF -> stringResource(R.string.settings_mediaservers_watch_off_done)
            SettingsMediaServersNoticeKind.WATCH_FAILED -> stringResource(R.string.settings_mediaservers_watch_failed)
            SettingsMediaServersNoticeKind.LABELS_SAVED -> stringResource(R.string.settings_mediaservers_labels_saved)
            SettingsMediaServersNoticeKind.LABELS_FAILED -> stringResource(R.string.settings_mediaservers_labels_failed)
            SettingsMediaServersNoticeKind.LABELS_STARTED -> stringResource(R.string.settings_mediaservers_labels_existing_started)
            SettingsMediaServersNoticeKind.LABELS_START_FAILED -> stringResource(R.string.settings_mediaservers_labels_existing_failed)
            SettingsMediaServersNoticeKind.TRACKS_SAVED -> stringResource(R.string.settings_mediaservers_tracks_saved)
            SettingsMediaServersNoticeKind.TRACKS_SAVED_ON -> stringResource(R.string.settings_mediaservers_tracks_saved_on)
            SettingsMediaServersNoticeKind.TRACKS_FAILED -> stringResource(R.string.settings_mediaservers_tracks_failed)
        }
    val error = notice.error ?: return head
    return listOf(head, errorText(error), notice.detail).filter { it.isNotEmpty() }.joinToString("\n")
}

@Composable
fun mediaServerTagText(tag: SettingsMediaServersTag): String {
    if (!tag.ok) return stringResource(R.string.settings_mediaservers_tag_not_connected)
    val libraries =
        if (tag.libraries > 0) pluralStringResource(R.plurals.settings_mediaservers_libraries, tag.libraries, tag.libraries) else ""
    return tag.label(stringResource(R.string.settings_mediaservers_tag_connected), libraries)
}

@Composable
fun mediaServerToldText(row: SettingsMediaServersRow): String {
    val told =
        if (row.toldNothing) {
            stringResource(R.string.settings_mediaservers_told_nothing)
        } else {
            val imports = stringResource(R.string.settings_mediaservers_told_imports)
            val renames = stringResource(R.string.settings_mediaservers_told_renames)
            val deletes = stringResource(R.string.settings_mediaservers_told_deletes)
            val list =
                listOf(imports to row.onImport, renames to row.onRename, deletes to row.onDelete)
                    .filter { it.second }
                    .joinToString(", ") { it.first }
            stringResource(R.string.settings_mediaservers_told_about, list)
        }
    return if (row.hasPathMap) "$told · ${stringResource(R.string.settings_mediaservers_path_map, row.pathFrom, row.pathTo)}" else told
}

@Composable
fun mediaServerLanguageName(
    code: String,
    fallback: String,
): String {
    if (code == OFF) return stringResource(R.string.settings_mediaservers_tracks_off)
    val locale = LocalConfiguration.current.locales[0]
    val name = runCatching { Locale.forLanguageTag(code).getDisplayLanguage(locale) }.getOrNull().orEmpty()
    if (name.isBlank() || name.equals(code, ignoreCase = true)) return fallback.ifBlank { code }
    return name.replaceFirstChar { it.titlecase(locale) }
}
