package ch.orellbuehler.acolyte.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.discover.CardKind
import ch.orellbuehler.acolyte.feature.home.AttentionKind
import ch.orellbuehler.acolyte.feature.home.AttentionRow
import ch.orellbuehler.acolyte.feature.home.AttentionTarget
import ch.orellbuehler.acolyte.feature.home.DownloadStage
import ch.orellbuehler.acolyte.feature.home.HomeState
import ch.orellbuehler.acolyte.feature.home.HomeTile
import ch.orellbuehler.acolyte.feature.home.HomeTileKind
import ch.orellbuehler.acolyte.feature.home.NewsKind
import ch.orellbuehler.acolyte.feature.home.PartCount
import ch.orellbuehler.acolyte.feature.home.SetupStepKind
import ch.orellbuehler.acolyte.feature.home.SystemDownload
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.ui.components.relativeTime
import ch.orellbuehler.acolyte.ui.discover.DiscoverNav
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte
import java.util.Locale

@Composable
fun newsText(state: HomeState): String? {
    val news = state.news ?: return null
    return when (news.kind) {
        NewsKind.ASKED_ARRIVED_ANY -> stringResource(R.string.home_news_asked_any)
        NewsKind.ASKED_ARRIVED_ONE -> stringResource(R.string.home_news_asked_one, news.title)
        NewsKind.ASKED_ARRIVED_MANY -> stringResource(R.string.home_news_asked_many, news.count)
        NewsKind.WAITING -> pluralStringResource(R.plurals.home_news_waiting, news.count, news.count)
        NewsKind.WAITING_PAGE -> pluralStringResource(R.plurals.home_news_waiting_page, news.count, news.count)
        NewsKind.NEW_TODAY_ANY -> stringResource(R.string.home_news_new_any)
        NewsKind.NEW_TODAY_ONE -> stringResource(R.string.home_news_new_one, news.title)
        NewsKind.NEW_TODAY_MANY -> stringResource(R.string.home_news_new_many, news.count)
        NewsKind.MINE_FINISHING -> stringResource(R.string.home_news_mine_finishing)
        NewsKind.FINISHING -> pluralStringResource(R.plurals.home_news_finishing, news.count, news.count)
    }
}

@Composable
fun stepTitle(kind: SetupStepKind): String =
    stringResource(
        when (kind) {
            SetupStepKind.FOLDER -> R.string.home_step_folder
            SetupStepKind.SOURCE -> R.string.home_step_source
            SetupStepKind.DOWNLOADER -> R.string.home_step_downloader
            SetupStepKind.TITLE -> R.string.home_step_title
        },
    )

@Composable
fun stepDetail(kind: SetupStepKind): String =
    stringResource(
        when (kind) {
            SetupStepKind.FOLDER -> R.string.home_step_folder_detail
            SetupStepKind.SOURCE -> R.string.home_step_source_detail
            SetupStepKind.DOWNLOADER -> R.string.home_step_downloader_detail
            SetupStepKind.TITLE -> R.string.home_step_title_detail
        },
    )

@Composable
fun stepAction(kind: SetupStepKind): String =
    stringResource(
        when (kind) {
            SetupStepKind.FOLDER -> R.string.home_step_folder_action
            SetupStepKind.SOURCE -> R.string.home_step_source
            SetupStepKind.DOWNLOADER -> R.string.home_step_downloader
            SetupStepKind.TITLE -> R.string.home_step_title_action
        },
    )

fun formatBytes(bytes: Long): String {
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (value < 10 &&
        unit > 0
    ) {
        String.format(Locale.getDefault(), "%.1f %s", value, units[unit])
    } else {
        "${value.toLong()} ${units[unit]}"
    }
}

fun formatDuration(seconds: Long): String =
    when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m"
        seconds < 48 * 3600 -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        else -> "${seconds / 86400}d ${(seconds % 86400) / 3600}h"
    }

@Composable
fun tileContent(
    tile: HomeTile,
    state: HomeState,
): TileContent {
    val colors = MaterialTheme.acolyte
    return when (tile.kind) {
        HomeTileKind.SETUP -> {
            val next = state.nextStep
            val label = stringResource(if (next?.kind == SetupStepKind.FOLDER) R.string.section_library else R.string.section_downloads)
            if (next == null || next.kind == SetupStepKind.TITLE) {
                TileContent(Icons.Rounded.VideoLibrary, label, stepTitle(SetupStepKind.TITLE), stepDetail(SetupStepKind.TITLE))
            } else {
                TileContent(Icons.Rounded.VideoLibrary, label, stringResource(R.string.home_tile_not_set_up), stepTitle(next.kind))
            }
        }
        HomeTileKind.ATTENTION -> {
            val att = state.attention
            val rows = att?.rows.orEmpty()
            val tint =
                when (att?.worst) {
                    Tone.FAIL -> colors.failText
                    Tone.WAIT -> colors.waitText
                    else -> colors.okText
                }
            val label = stringResource(R.string.home_tile_attention)
            when {
                !state.settled -> TileContent(Icons.Rounded.WarningAmber, label, "…", "")
                rows.isNotEmpty() -> {
                    val first = rows.first()
                    val more = att?.more ?: 0
                    val line = attentionTitle(first) + ": " + attentionReason(first)
                    TileContent(
                        Icons.Rounded.WarningAmber,
                        label,
                        rows.sumOf { it.count }.toString(),
                        if (more > 0) stringResource(R.string.home_tile_and_more, line, more) else line,
                        tint,
                    )
                }
                att?.unchecked == true ->
                    TileContent(
                        Icons.Rounded.WarningAmber,
                        label,
                        stringResource(R.string.home_could_not_check),
                        stringResource(R.string.home_try_again),
                    )
                else ->
                    TileContent(
                        Icons.Rounded.CheckCircle,
                        label,
                        stringResource(R.string.home_all_good),
                        stringResource(R.string.home_all_good_line),
                        tint,
                    )
            }
        }
        HomeTileKind.ASKS -> {
            val asks = state.asks
            val waiting = asks?.approver == true && asks.waiting > 0
            val label =
                stringResource(
                    when {
                        waiting -> R.string.home_tile_to_approve
                        (asks?.open ?: 0) > 0 -> R.string.home_tile_your_asks
                        else -> R.string.section_requests
                    },
                )
            when {
                asks == null || asks.failed ->
                    TileContent(
                        Icons.Rounded.Checklist,
                        label,
                        stringResource(R.string.home_could_not_read),
                        stringResource(R.string.home_could_not_read_asks),
                    )
                waiting ->
                    TileContent(
                        Icons.Rounded.Checklist,
                        label,
                        asks.waiting.toString(),
                        pluralStringResource(R.plurals.home_tile_to_look_at, asks.waiting, asks.waiting),
                        posters = asks.posters,
                    )
                asks.open > 0 -> {
                    val open = stringResource(R.string.home_tile_still_open)
                    val small =
                        if (asks.declined >
                            0
                        ) {
                            open + " · " + stringResource(R.string.home_tile_turned_down, asks.declined)
                        } else {
                            open
                        }
                    TileContent(Icons.Rounded.Checklist, label, asks.open.toString(), small)
                }
                asks.declined > 0 ->
                    TileContent(
                        Icons.Rounded.Checklist,
                        label,
                        asks.declined.toString(),
                        stringResource(R.string.home_tile_turned_down_word),
                    )
                else ->
                    TileContent(
                        Icons.Rounded.Checklist,
                        label,
                        stringResource(R.string.home_tile_nothing_waiting),
                        stringResource(if (asks.approver) R.string.home_tile_no_asks else R.string.home_tile_ask_on_explore),
                    )
            }
        }
        HomeTileKind.LIBRARY -> {
            val kinds = state.library.filter { it.titles > 0 }.ifEmpty { state.library.take(1) }
            val small =
                if (kinds.size == 1) {
                    val kind = kinds.first()
                    when (kind.kind) {
                        CardKind.SHOW ->
                            pluralStringResource(R.plurals.home_seasons, kind.parts, kind.parts) + " · " +
                                pluralStringResource(R.plurals.home_episodes, kind.items, kind.items)
                        CardKind.ARTIST ->
                            pluralStringResource(R.plurals.home_albums, kind.parts, kind.parts) + " · " +
                                pluralStringResource(R.plurals.home_tracks, kind.items, kind.items)
                        CardKind.MOVIE -> pluralStringResource(R.plurals.home_movies, kind.titles, kind.titles)
                    }
                } else {
                    kinds
                        .map { kind ->
                            when (kind.kind) {
                                CardKind.MOVIE -> pluralStringResource(R.plurals.home_movies, kind.titles, kind.titles)
                                CardKind.SHOW -> pluralStringResource(R.plurals.home_shows, kind.titles, kind.titles)
                                CardKind.ARTIST -> pluralStringResource(R.plurals.home_artists, kind.titles, kind.titles)
                            }
                        }.joinToString(" · ")
                }
            TileContent(Icons.Rounded.VideoLibrary, stringResource(R.string.section_library), state.libraryTotal.toString(), small)
        }
        HomeTileKind.DISK -> {
            val disk = state.disk
            val free = disk?.free
            val small =
                when {
                    disk == null || disk.failed -> stringResource(R.string.home_disk_unread)
                    free == null -> stringResource(R.string.home_disk_across)
                    free.free == 0L -> stringResource(R.string.home_disk_full)
                    free.many -> stringResource(R.string.home_disk_free_fullest, formatBytes(free.free))
                    else -> stringResource(R.string.home_disk_free, formatBytes(free.free))
                }
            val tint =
                when (disk?.tone) {
                    Tone.FAIL -> colors.failText
                    Tone.WAIT -> colors.waitText
                    else -> null
                }
            TileContent(Icons.Rounded.Archive, stringResource(R.string.home_tile_disk), formatBytes(disk?.size ?: 0), small, tint)
        }
        HomeTileKind.WEEK -> {
            val week = state.week
            TileContent(
                Icons.Rounded.Event,
                stringResource(R.string.home_tile_week),
                (week?.count ?: 0).toString(),
                stringResource(if (week?.borrowed == true) R.string.home_tile_week_borrowed else R.string.home_tile_week_own),
                posters = week?.posters.orEmpty(),
            )
        }
        HomeTileKind.DOWNLOADS -> {
            val dl = state.downloads
            val words =
                listOfNotNull(
                    dl?.downloading?.takeIf { it > 0 }?.let { stringResource(R.string.home_stage_downloading, it) },
                    dl?.importing?.takeIf { it > 0 }?.let { stringResource(R.string.home_stage_importing, it) },
                    dl?.waiting?.takeIf { it > 0 }?.let { stringResource(R.string.home_stage_waiting, it) },
                    dl?.paused?.takeIf { it > 0 }?.let { stringResource(R.string.home_stage_paused, it) },
                )
            TileContent(
                Icons.Rounded.Download,
                stringResource(R.string.section_downloads),
                (dl?.fetching ?: 0).toString(),
                words.joinToString(" · "),
            )
        }
        HomeTileKind.NEW -> {
            val small =
                when {
                    state.newArrivals > 0 && state.newSeasons > 0 -> stringResource(R.string.home_new_both)
                    state.newArrivals > 0 -> stringResource(R.string.home_new_arrivals)
                    else -> pluralStringResource(R.plurals.home_new_seasons, state.newSeasons)
                }
            TileContent(
                Icons.Rounded.AutoAwesome,
                stringResource(R.string.home_tile_new),
                (state.newArrivals + state.newSeasons).toString(),
                small,
            )
        }
        HomeTileKind.EXPLORE ->
            TileContent(
                Icons.Rounded.Explore,
                stringResource(R.string.section_explore),
                stringResource(R.string.home_tile_explore_big),
                stringResource(R.string.home_tile_explore_small),
            )
        HomeTileKind.POPULAR ->
            TileContent(
                Icons.Rounded.Star,
                stringResource(R.string.section_popular),
                stringResource(
                    if (state.popularBuilding &&
                        state.popularArt == null
                    ) {
                        R.string.home_getting_ready
                    } else {
                        R.string.home_tile_popular_big
                    },
                ),
                stringResource(R.string.home_tile_popular_small),
            )
        HomeTileKind.CALENDAR ->
            TileContent(
                Icons.Rounded.CalendarMonth,
                stringResource(R.string.section_calendar),
                stringResource(R.string.home_tile_calendar_big),
                stringResource(R.string.home_tile_calendar_small),
            )
        HomeTileKind.SEARCH ->
            TileContent(
                Icons.Rounded.Search,
                stringResource(R.string.section_search),
                stringResource(R.string.home_tile_search_big),
                stringResource(R.string.home_tile_search_small),
            )
    }
}

@Composable
fun attentionTitle(row: AttentionRow): String =
    when (row.kind) {
        AttentionKind.DOWNLOAD_FAILED, AttentionKind.IMPORT_REFUSED ->
            if (row.count > 1) pluralStringResource(R.plurals.home_downloads_failed, row.count, row.count) else row.title
        AttentionKind.DECISION -> if (row.count > 1) pluralStringResource(R.plurals.home_need_decision, row.count, row.count) else row.title
        AttentionKind.HEALTH, AttentionKind.SETUP -> healthWord(row.checkKind, row.count)
    }

@Composable
fun attentionReason(row: AttentionRow): String =
    when (row.kind) {
        AttentionKind.DOWNLOAD_FAILED ->
            if (row.count >
                1
            ) {
                row.names.joinToString(", ")
            } else {
                stringResource(R.string.home_reason_download_failed)
            }
        AttentionKind.IMPORT_REFUSED -> stringResource(R.string.home_reason_import_refused)
        AttentionKind.DECISION -> if (row.count > 1) row.names.joinToString(", ") else stringResource(R.string.home_reason_decision)
        AttentionKind.HEALTH, AttentionKind.SETUP -> {
            val reason = healthReason(row.checkKind, row.error, row.count > 1)
            val names = row.names.joinToString(", ")
            if (names.isEmpty()) reason else "$names: $reason"
        }
    }

@Composable
private fun healthWord(
    kind: String,
    count: Int,
): String {
    val one =
        when (kind) {
            "library_folder", "disk_space" -> R.string.home_check_library_folder
            "source" -> R.string.home_check_source
            "downloader", "save_folder", "no_downloader", "same_downloader" -> R.string.home_check_downloader
            "backups" -> R.string.home_check_backups
            "update" -> R.string.home_check_update
            "config_disk", "config_folder" -> R.string.home_check_config
            "waiting_downloads", "clear_waiting" -> R.string.section_downloads
            "key_kit" -> R.string.home_check_master_key
            "trailers" -> R.string.home_check_trailers
            "people_check" -> R.string.home_check_people
            else -> R.string.home_check_holyarrs
        }
    val text = stringResource(one)
    return if (count > 1) "$count × $text" else text
}

@Composable
private fun healthReason(
    kind: String,
    error: Boolean,
    many: Boolean,
): String =
    stringResource(
        when (kind) {
            "library_folder" -> if (error) R.string.home_reason_unreachable else R.string.home_reason_needs_look
            "disk_space" -> if (error) R.string.home_reason_out_of_space else R.string.home_reason_low_space
            "source", "downloader" -> if (error) R.string.home_reason_not_answering else R.string.home_reason_problem
            "backups" -> if (error) R.string.home_reason_backups_missing else R.string.home_reason_backups_old
            "update" -> if (error) R.string.home_reason_check_failed else R.string.home_reason_waiting
            "config_disk" -> if (error) R.string.home_reason_almost_full else R.string.home_reason_low_space
            "config_folder" -> if (error) R.string.home_reason_container else R.string.home_reason_needs_look
            "waiting_downloads" -> R.string.home_reason_waiting_files
            "clear_waiting" -> R.string.home_reason_waiting_cleared
            "save_folder" -> if (many) R.string.home_reason_no_save_folder_many else R.string.home_reason_no_save_folder
            "no_downloader" -> R.string.home_reason_no_downloader
            "same_downloader" -> if (many) R.string.home_reason_added_twice_many else R.string.home_reason_added_twice
            "key_kit" -> R.string.home_reason_no_key_kit
            "trailers" -> if (error) R.string.home_reason_not_loading else R.string.home_reason_lookups_failing
            "people_check" -> R.string.home_reason_users
            else -> if (error) R.string.home_reason_not_working else R.string.home_reason_needs_look
        },
    )

@Composable
private fun actionWord(row: AttentionRow): String =
    stringResource(
        when {
            row.kind == AttentionKind.DECISION -> R.string.home_action_decide
            row.target == AttentionTarget.DOWNLOADS -> R.string.section_downloads
            row.target == AttentionTarget.SETTINGS -> R.string.home_action_fix
            else -> R.string.home_action_look
        },
    )

private fun AttentionTarget.section(): Section =
    when (this) {
        AttentionTarget.SETTINGS -> Section.SETTINGS
        AttentionTarget.HEALTH -> Section.HEALTH
        AttentionTarget.DOWNLOADS -> Section.DOWNLOADS
    }

fun LazyListScope.systemBoard(
    state: HomeState,
    nav: DiscoverNav,
) {
    val board = state.systemBoard ?: return
    item(key = "sys-downloads") {
        BoardCard(onClick = { nav.openSection(Section.DOWNLOADS) }) {
            Text(stringResource(R.string.home_sys_downloading), style = MaterialTheme.typography.titleMedium)
            when {
                board.downloadsFailed -> {
                    Text(stringResource(R.string.home_could_not_read), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.home_could_not_read_downloads), style = MaterialTheme.typography.bodySmall)
                }
                else -> {
                    val active = board.downloading + board.importing + board.waiting
                    Text(
                        if (board.speed >
                            0
                        ) {
                            "${formatBytes(board.speed.toLong())}/s"
                        } else {
                            stringResource(R.string.home_stage_downloading, board.downloading)
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Large)) {
                        Counter(stringResource(R.string.home_sys_waiting), board.waiting)
                        Counter(stringResource(R.string.home_sys_downloading), board.downloading)
                        Counter(stringResource(R.string.home_sys_importing), board.importing)
                    }
                    board.running.forEach { RunningLine(it) }
                    if (active == 0) {
                        if (board.finished.isNotEmpty()) {
                            board.finished.forEach { done ->
                                Text(
                                    done.name +
                                        (
                                            relativeTime(
                                                done.finishedAt,
                                            )?.let { " · " + stringResource(R.string.home_sys_finished, it) } ?: ""
                                        ),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        } else {
                            Text(stringResource(R.string.home_sys_idle), style = MaterialTheme.typography.bodySmall)
                        }
                        if (board.paused >
                            0
                        ) {
                            Text(stringResource(R.string.home_sys_paused, board.paused), style = MaterialTheme.typography.bodySmall)
                        }
                        if (board.notSent >
                            0
                        ) {
                            Text(stringResource(R.string.home_sys_not_sent, board.notSent), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
    item(key = "sys-attention") {
        BoardCard(onClick = null) {
            Text(stringResource(R.string.home_tile_attention), style = MaterialTheme.typography.titleMedium)
            when {
                board.attention.isNotEmpty() -> {
                    board.attention.take(MAX_ROWS).forEach { row ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = Spacing.XSmall),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                        ) {
                            Box(Modifier.size(8.dp).background(MaterialTheme.acolyte.dot(row.tone), CircleShape))
                            Column(Modifier.weight(1f)) {
                                Text(attentionTitle(row), style = MaterialTheme.typography.titleSmall)
                                Text(
                                    attentionReason(row),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            androidx.compose.material3.TextButton(
                                onClick = { nav.openSection(row.target.section()) },
                            ) { Text(actionWord(row)) }
                        }
                    }
                    val hidden = board.attention.size - MAX_ROWS
                    if (hidden > 0) Text(stringResource(R.string.home_sys_more, hidden), style = MaterialTheme.typography.bodySmall)
                    if (board.attention.size <= 2 && board.passed > 0) {
                        Text(
                            pluralStringResource(R.plurals.home_sys_passed, board.passed, board.passed),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                board.unchecked -> {
                    Text(stringResource(R.string.home_could_not_check), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.home_sys_unchecked), style = MaterialTheme.typography.bodySmall)
                }
                else -> {
                    Text(
                        stringResource(R.string.home_all_good),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.acolyte.okText,
                    )
                    Text(stringResource(R.string.home_sys_all_good), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    item(key = "sys-parts") {
        Row(Modifier.padding(horizontal = Spacing.Screen), horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            PartTile(
                stringResource(R.string.home_check_sources),
                board.sources,
                board.sourcesOff,
                R.string.home_sys_add_source,
                Modifier.weight(1f),
            ) {
                nav.openSection(Section.SETTINGS)
            }
            PartTile(
                stringResource(R.string.home_check_downloaders),
                board.downloaders,
                false,
                R.string.home_sys_add_downloader,
                Modifier.weight(1f),
            ) {
                nav.openSection(Section.SETTINGS)
            }
        }
    }
    item(key = "sys-uptime") {
        BoardCard(onClick = { nav.openSection(Section.HEALTH) }) {
            Text(stringResource(R.string.home_sys_running_for), style = MaterialTheme.typography.titleMedium)
            Text(formatDuration(board.uptimeSeconds), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (board.version.isNotEmpty()) {
                Text(
                    stringResource(R.string.home_sys_version, board.version),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private const val MAX_ROWS = 4

@Composable
private fun BoardCard(
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    val modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small)
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    val inner: @Composable () -> Unit = {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) { content() }
    }
    if (onClick !=
        null
    ) {
        Card(onClick = onClick, colors = colors, modifier = modifier) { inner() }
    } else {
        Card(colors = colors, modifier = modifier) { inner() }
    }
}

@Composable
private fun Counter(
    label: String,
    value: Int,
) {
    Column {
        Text(value.toString(), style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RunningLine(download: SystemDownload) {
    Column(Modifier.fillMaxWidth()) {
        val percent = (download.progress * 100).toInt().coerceIn(0, 100)
        val state =
            when (download.stage) {
                DownloadStage.DOWNLOADING ->
                    if (download.speed > 0) "$percent% · ${formatBytes(download.speed.toLong())}/s" else "$percent%"
                DownloadStage.QUEUED -> stringResource(R.string.home_sys_waiting)
                DownloadStage.IMPORTING -> stringResource(R.string.home_sys_importing)
                DownloadStage.CHECKING -> stringResource(R.string.home_sys_checking)
                DownloadStage.STALLED -> stringResource(R.string.home_sys_stalled, percent)
                else -> ""
            }
        Text(download.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
        Text(state, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (download.stage == DownloadStage.DOWNLOADING) {
            LinearProgressIndicator(
                progress = { download.progress.toFloat() },
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.XSmall),
            )
        }
    }
}

@Composable
private fun PartTile(
    label: String,
    count: PartCount?,
    off: Boolean,
    add: Int,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.padding(vertical = Spacing.Small),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            when {
                off -> {
                    Text(stringResource(R.string.home_sys_off), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.home_sys_off_parts), style = MaterialTheme.typography.bodySmall)
                }
                count == null -> {
                    Text(stringResource(R.string.home_could_not_read), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.home_could_not_read_parts), style = MaterialTheme.typography.bodySmall)
                }
                count.total == 0 -> {
                    Text(stringResource(R.string.home_sys_none_added), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(add), style = MaterialTheme.typography.bodySmall)
                }
                else -> {
                    Text(stringResource(R.string.home_sys_working, count.working, count.total), style = MaterialTheme.typography.titleLarge)
                    Text(
                        when {
                            count.troubled.size == 1 -> stringResource(R.string.home_sys_has_problem, count.troubled.first())
                            count.troubled.size > 1 -> stringResource(R.string.home_sys_have_problem, count.troubled.size)
                            count.off > 0 -> stringResource(R.string.home_sys_turned_off, count.off)
                            else -> stringResource(R.string.home_sys_all_working)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            if (count.troubled.isEmpty()) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.acolyte.failText
                            },
                    )
                }
            }
        }
    }
}
