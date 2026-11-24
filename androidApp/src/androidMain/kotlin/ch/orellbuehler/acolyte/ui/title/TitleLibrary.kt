package ch.orellbuehler.acolyte.ui.title

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.model.OwnedVersion
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.title.LibraryKind
import ch.orellbuehler.acolyte.feature.title.ReleaseScope
import ch.orellbuehler.acolyte.feature.title.ReleaseScopeKind
import ch.orellbuehler.acolyte.feature.title.SeasonPick
import ch.orellbuehler.acolyte.feature.title.SlotCard
import ch.orellbuehler.acolyte.feature.title.StatusButton
import ch.orellbuehler.acolyte.feature.title.StatusSub
import ch.orellbuehler.acolyte.feature.title.SubtitleMode
import ch.orellbuehler.acolyte.feature.title.TitleState
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.NoticeBanner
import ch.orellbuehler.acolyte.ui.components.SectionHeader
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.relativeTime
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

fun LazyListScope.libraryContent(
    state: TitleState,
    viewModel: TitleViewModel,
    links: TitleNav,
) {
    val owned = state.owned ?: return
    state.ownedError?.let { error -> item(key = "owned-error") { ErrorBanner(error) } }
    if (state.access.manage && owned.noDownloader.isNotBlank()) {
        item(key = "no-downloader") {
            NoticeBanner(
                stringResource(if (owned.noDownloader == "usenet") R.string.title_no_usenet else R.string.title_no_torrent),
            )
        }
    }
    item(key = "status") { StatusCard(state, viewModel) }
    if (state.isShow) {
        item(key = "seasons") { ShowSeasons(state, viewModel) }
        if (owned.versions.size > 1 || state.access.manage) item(key = "show-versions") { ShowVersions(state, viewModel) }
    } else {
        item(key = "versions") { MovieVersions(state, viewModel) }
    }
    if (state.access.manage && state.held.isNotEmpty()) item(key = "held") { HeldLine(state, viewModel) }
    if (state.activeDownloads.isNotEmpty()) item(key = "downloads") { Downloading(state, viewModel) }
    item(key = "history") { HistoryCard(state, viewModel) }
    if (state.access.manage) {
        item(key = "settings") { SettingsCard(state, viewModel) }
        item(key = "delete") {
            ListItem(
                headlineContent = {
                    Text(
                        stringResource(if (state.isShow) R.string.title_delete_show else R.string.title_delete_movie),
                        color = MaterialTheme.acolyte.failText,
                    )
                },
                modifier = Modifier.clickable { viewModel.openRemove() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusCard(
    state: TitleState,
    viewModel: TitleViewModel,
) {
    val owned = state.owned ?: return
    val status = state.libraryStatus ?: return
    val loc = locale()
    LibraryCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Box(Modifier.size(10.dp).background(MaterialTheme.acolyte.dot(status.tone), CircleShape))
            Text(
                if (status.kind == LibraryKind.HAVE &&
                    !state.isShow &&
                    status.label == ch.orellbuehler.acolyte.feature.title.LibraryLabel.IN_LIBRARY
                ) {
                    stringResource(R.string.title_in_your_library)
                } else {
                    libraryLabel(status)
                },
                style = MaterialTheme.typography.titleMedium,
            )
        }
        val sub =
            when (state.statusSub) {
                StatusSub.LEFT_IN_DOWNLOADER -> stringResource(R.string.title_sub_left_in_downloader)
                StatusSub.DOWNLOADING_HANDS_OFF -> stringResource(R.string.title_sub_downloading_hands_off)
                StatusSub.FULL_SEASONS ->
                    (
                        listOf(pluralStringResource(R.plurals.title_sub_full_seasons, state.fullSeasons, state.fullSeasons)) +
                            formatBytes(owned.sizeBytes, loc)
                    ).filter { it.isNotBlank() }
                        .joinToString(" · ")
                StatusSub.HAVE_MOVIE ->
                    listOf(
                        state.ownBadges.joinToString(" · "),
                        formatBytes(state.mainFile?.size ?: 0, loc),
                        owned.addedAt
                            .takeIf { it.isNotBlank() }
                            ?.let {
                                stringResource(
                                    R.string.title_sub_added,
                                    relativeTime(it).orEmpty(),
                                )
                            }.orEmpty(),
                    ).filter { it.isNotBlank() }.joinToString(" · ")
                StatusSub.ON_ITS_WAY -> stringResource(R.string.title_sub_on_its_way)
                StatusSub.LAST_TRY_FAILED -> stringResource(R.string.title_sub_last_try_failed)
                StatusSub.WAITS_FOR_COPY -> stringResource(R.string.title_sub_waits)
                StatusSub.NOT_KEPT -> stringResource(R.string.title_sub_not_kept)
                null -> ""
            }
        if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.statusPercent?.let { percent ->
            LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            if (state.folderName.isNotBlank()) AssistChip(onClick = {}, label = { Text(state.folderName, maxLines = 1) })
            if (owned.sizeBytes > 0) AssistChip(onClick = {}, label = { Text(formatBytes(owned.sizeBytes, loc)) })
            if (owned.pickRulesName.isNotBlank()) AssistChip(onClick = {}, label = { Text(owned.pickRulesName) })
            AssistChip(
                onClick = {},
                label = { Text(stringResource(if (owned.keepUpToDate) R.string.title_kept else R.string.title_not_kept)) },
            )
            owned.show?.status?.takeIf { it.isNotBlank() }?.let {
                AssistChip(onClick = {}, label = {
                    Text(
                        it.replaceFirstChar { c ->
                            c.uppercase()
                        },
                    )
                })
            }
            owned.show
                ?.network
                ?.takeIf { it.isNotBlank() }
                ?.let { AssistChip(onClick = {}, label = { Text(it) }) }
            if (owned.originalLanguage.isNotBlank()) AssistChip(onClick = {}, label = { Text(languageName(owned.originalLanguage, loc)) })
        }
        if (state.access.manage) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                when (state.statusButton) {
                    StatusButton.TRY_AGAIN ->
                        Button(onClick = { viewModel.launch { viewModel.presenter.searchNow() } }, enabled = state.busy != "search") {
                            Text(stringResource(R.string.title_try_again))
                        }
                    StatusButton.SEARCH_NOW ->
                        Button(onClick = { viewModel.launch { viewModel.presenter.searchNow() } }, enabled = state.busy != "search") {
                            Text(stringResource(R.string.title_search_now))
                        }
                    StatusButton.NONE -> Unit
                }
                OutlinedButton(onClick = { viewModel.openReleases(ReleaseScope(ReleaseScopeKind.WHOLE)) }) {
                    Text(stringResource(R.string.title_choose_release))
                }
            }
        }
    }
}

@Composable
fun LibraryCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) { content() }
    }
}

@Composable
private fun MovieVersions(
    state: TitleState,
    viewModel: TitleViewModel,
) {
    val owned = state.owned ?: return
    Column {
        SectionHeader(
            stringResource(if (state.movieFileHeading) R.string.title_file else R.string.title_versions),
            actionLabel = if (state.access.manage) stringResource(R.string.title_add_version) else null,
            onAction =
                if (state.access.manage) {
                    {
                        viewModel.launch { viewModel.presenter.loadChoices() }
                        viewModel.open(TitleSheet.VersionForm(null))
                    }
                } else {
                    null
                },
        )
        if (state.slotCards.isEmpty()) {
            Text(stringResource(R.string.title_no_file), modifier = Modifier.padding(horizontal = Spacing.Screen))
        }
        state.slotCards.forEach { card ->
            SlotCardView(card, owned.versions.firstOrNull { it.id == card.versionId }, state, viewModel)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SlotCardView(
    card: SlotCard,
    version: OwnedVersion?,
    state: TitleState,
    viewModel: TitleViewModel,
) {
    val file = card.slot.file
    val loc = locale()
    LibraryCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (card.main && (card.label.isBlank() || card.label == "Main")) stringResource(R.string.title_main_copy) else card.label,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (state.access.manage && (!card.main || file != null || state.legacy)) {
                CopyMenu(card, version, state, viewModel)
            }
        }
        val badges = card.badges + if (file?.isProtected == true) listOf(stringResource(R.string.title_protected)) else emptyList()
        if (badges.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                badges.forEach { StatusChip(it, Tone.IDLE) }
            }
        }
        if (file != null) {
            InfoLine(stringResource(R.string.title_quality), file.resolutionLabel)
            InfoLine(stringResource(R.string.title_size), formatBytes(file.size, loc))
            if (file.addedAt.isNotBlank()) InfoLine(stringResource(R.string.title_added), formatDate(file.addedAt, loc))
            if (file.audioLanguages.isNotEmpty()) {
                InfoLine(
                    stringResource(R.string.title_audio),
                    file.audioLanguages.take(6).joinToString(" "),
                )
            }
            if (file.subtitleLanguages.isNotEmpty()) {
                InfoLine(
                    stringResource(R.string.title_subtitles),
                    file.subtitleLanguages.take(6).joinToString(" "),
                )
            }
            Text(file.fileName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.access.manage) {
                PointsView(
                    state.points[card.slot.id],
                ) { viewModel.launch { viewModel.presenter.loadPoints(card.slot.id) } }
            }
        } else if (ch.orellbuehler.acolyte.feature.title.TitleLogic
                .leftInDownloader(card.slot)
        ) {
            Text(stringResource(R.string.title_left_in_downloader), style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(stringResource(R.string.title_no_file_yet, card.slot.stateLabel), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CopyMenu(
    card: SlotCard,
    version: OwnedVersion?,
    state: TitleState,
    viewModel: TitleViewModel,
) {
    var open by remember { mutableStateOf(false) }
    val file = card.slot.file
    val presenter = viewModel.presenter
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.title_version_actions, card.label))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (file != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.title_set_edition)) },
                    onClick = {
                        open = false
                        viewModel.open(TitleSheet.Edition(file.id, file.edition))
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(if (file.isProtected) R.string.title_unprotect else R.string.title_protect)) },
                    onClick = {
                        open = false
                        if (file.isProtected) {
                            viewModel.open(
                                TitleSheet.Unprotect(file.id),
                            )
                        } else {
                            viewModel.launch { presenter.protect(file.id, true) }
                        }
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.title_keep_and_add)) },
                    onClick = {
                        open = false
                        viewModel.launch { presenter.loadChoices() }
                        viewModel.open(TitleSheet.VersionForm(null, keepFileId = file.id))
                    },
                )
            } else if (state.legacy && version != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.title_adopt)) },
                    onClick = {
                        open = false
                        viewModel.open(TitleSheet.Adopt(version.id))
                    },
                )
            }
            if (card.removable && version != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.title_remove_version, version.label), color = MaterialTheme.acolyte.failText) },
                    onClick = {
                        open = false
                        viewModel.open(TitleSheet.RemoveVersion(version))
                    },
                )
            }
        }
    }
}

@Composable
fun PointsView(
    points: ch.orellbuehler.acolyte.api.model.FilePoints?,
    load: () -> Unit,
) {
    if (points == null) {
        TextButton(onClick = load) { Text(stringResource(R.string.title_points_show)) }
        return
    }
    val need = points.goodEnoughPoints
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Text(
            if (need !=
                null
            ) {
                stringResource(R.string.title_points_of, points.points, need)
            } else {
                stringResource(R.string.title_points, points.points)
            },
            style = MaterialTheme.typography.titleSmall,
        )
        StatusChip(
            stringResource(if (points.goodEnough) R.string.title_good_enough else R.string.title_below_good_enough),
            if (points.goodEnough) Tone.OK else Tone.WAIT,
        )
    }
    if (!points.goodEnough) {
        if (!points.stepReached) {
            Text(
                stringResource(R.string.title_points_step, stepName(points.step), stepName(points.goodEnoughAt)),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (need != null && points.points < need) {
            Text(stringResource(R.string.title_points_short, need - points.points), style = MaterialTheme.typography.bodySmall)
        }
        if (!points.meetsVersion) Text(stringResource(R.string.title_points_version), style = MaterialTheme.typography.bodySmall)
    }
    if (points.traits.isEmpty()) {
        Text(
            stringResource(R.string.title_points_no_traits),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Text(
            points.traits.joinToString(" · ") { t ->
                val name = points.names[t.trait] ?: t.trait.removePrefix("own:").replace('_', ' ')
                if (t.points >= 0) "$name +${t.points}" else "$name ${t.points}"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun stepName(step: String): String =
    when (step) {
        "" -> stringResource(R.string.title_step_unknown)
        "cam" -> stringResource(R.string.title_step_cam)
        "telesync" -> stringResource(R.string.title_step_telesync)
        "sd_dvd" -> stringResource(R.string.title_step_sd_dvd)
        "sd_tv" -> stringResource(R.string.title_step_sd_tv)
        else -> {
            val parts = step.split('_')
            val res = parts.firstOrNull().orEmpty().let { if (it == "2160p") "4K" else it }
            val source =
                when (parts.getOrNull(1)) {
                    "bluray" -> "Blu-ray"
                    "remux" -> "Remux"
                    "web" -> "Web"
                    "tv" -> "TV"
                    else -> parts.drop(1).joinToString(" ")
                }
            "$res $source".trim()
        }
    }

@Composable
private fun ShowVersions(
    state: TitleState,
    viewModel: TitleViewModel,
) {
    val owned = state.owned ?: return
    var all by rememberSaveable { mutableStateOf(false) }
    val versions = if (all) owned.versions else owned.versions.take(1)
    Column {
        SectionHeader(
            stringResource(R.string.title_versions),
            actionLabel = if (state.access.manage) stringResource(R.string.title_add_version) else null,
            onAction =
                if (state.access.manage) {
                    {
                        viewModel.launch { viewModel.presenter.loadChoices() }
                        viewModel.open(TitleSheet.VersionForm(null))
                    }
                } else {
                    null
                },
        )
        versions.forEach { version ->
            LibraryCard {
                Text(
                    if (version.main) stringResource(R.string.title_version_main, version.label) else version.label,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(
                        R.string.title_version_audio,
                        version.audioLanguages.joinToString(", ").ifBlank { stringResource(R.string.title_version_any) },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    stringResource(R.string.title_version_rules, version.pickRulesName) +
                        if (!version.keepUpToDate) " · " + stringResource(R.string.title_not_kept) else "",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (version.folderPath.isNotBlank()) {
                    Text(
                        version.folderPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.access.manage) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        TextButton(onClick = {
                            viewModel.launch { viewModel.presenter.loadChoices() }
                            viewModel.open(TitleSheet.VersionForm(version))
                        }) { Text(stringResource(R.string.title_change)) }
                        if (!version.main) {
                            TextButton(onClick = { viewModel.open(TitleSheet.RemoveVersion(version)) }) {
                                Text(stringResource(R.string.title_remove_version, version.label), color = MaterialTheme.acolyte.failText)
                            }
                        }
                    }
                }
            }
        }
        if (owned.versions.size > 1) {
            TextButton(onClick = { all = !all }, modifier = Modifier.padding(horizontal = Spacing.Small)) {
                Text(
                    if (all) {
                        stringResource(
                            R.string.title_show_less,
                        )
                    } else {
                        stringResource(R.string.title_see_all_count, owned.versions.size)
                    },
                )
            }
        }
        if (state.access.manage) {
            Text(
                stringResource(R.string.title_versions_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.Screen),
            )
        }
    }
}

@Composable
private fun HeldLine(
    state: TitleState,
    viewModel: TitleViewModel,
) {
    val loc = locale()
    LibraryCard {
        Text(
            pluralStringResource(R.plurals.title_held_copies, state.held.size, state.held.size) +
                state.heldDaysLeft
                    .takeIf { it.isNotBlank() }
                    ?.let {
                        " · " +
                            stringResource(
                                R.string.title_held_until,
                                formatDate(it, loc),
                            )
                    }.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = { viewModel.launch { viewModel.presenter.restoreHeld() } }, enabled = state.busy != "restore") {
            Text(stringResource(R.string.title_restore))
        }
    }
}

@Composable
private fun Downloading(
    state: TitleState,
    viewModel: TitleViewModel,
) {
    Column {
        SectionHeader(stringResource(R.string.title_downloading))
        state.activeDownloads.forEach { slot ->
            val download = slot.download ?: return@forEach
            LibraryCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(download.label.ifBlank { slot.version }, style = MaterialTheme.typography.titleSmall)
                        if (download.text.isNotBlank()) Text(download.text, style = MaterialTheme.typography.bodySmall)
                    }
                    StatusChip("${(download.progress.coerceIn(0.0, 1.0) * 100).toInt()}%", Tone.INFO)
                }
                LinearProgressIndicator(progress = { download.progress.toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { viewModel.open(TitleSheet.MarkFailed(download.id)) }) {
                    Text(stringResource(R.string.title_mark_failed), color = MaterialTheme.acolyte.failText)
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    state: TitleState,
    viewModel: TitleViewModel,
) {
    var all by rememberSaveable { mutableStateOf(false) }
    val rows = state.history
    Column {
        SectionHeader(stringResource(R.string.title_history))
        if (rows.isEmpty()) {
            Text(
                stringResource(R.string.title_history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.Screen),
            )
        }
        (if (all) rows else rows.take(5)).forEach { row ->
            HistoryLine(row.event, row.tone) { viewModel.open(TitleSheet.History) }
        }
        if (rows.size > 5) {
            TextButton(onClick = { all = !all }, modifier = Modifier.padding(horizontal = Spacing.Small)) {
                Text(if (all) stringResource(R.string.title_show_less) else stringResource(R.string.title_show_all_count, rows.size))
            }
        }
    }
}

@Composable
fun HistoryLine(
    event: ch.orellbuehler.acolyte.api.model.TitleEvent,
    tone: Tone,
    onClick: (() -> Unit)? = null,
) {
    ListItem(
        leadingContent = { Box(Modifier.size(8.dp).background(MaterialTheme.acolyte.dot(tone), CircleShape)) },
        headlineContent = {
            Text(
                event.summary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            val meta = listOfNotNull(event.episode.ifBlank { null }, event.version.ifBlank { null }, relativeTime(event.at))
            if (meta.isNotEmpty()) Text(meta.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
        },
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun SettingsCard(
    state: TitleState,
    viewModel: TitleViewModel,
) {
    val owned = state.owned ?: return
    val presenter = viewModel.presenter
    Column {
        SectionHeader(stringResource(R.string.title_settings))
        LibraryCard {
            SettingSwitch(
                stringResource(R.string.title_keep_up_to_date),
                stringResource(R.string.title_keep_hint),
                owned.keepUpToDate,
                enabled = state.busy != "keep",
            ) { viewModel.launch { presenter.toggleKeep() } }
            if (state.gateNote) {
                NoteLine(
                    stringResource(R.string.title_not_looking_yet),
                    owned.gateReason +
                        owned.gateOpensAt
                            .takeIf { it.isNotBlank() }
                            ?.let { " · " + formatDate(it, locale()) }
                            .orEmpty(),
                )
            }
            HorizontalDivider()
            QualityRow(state, viewModel)
            if (!state.isShow) {
                MenuRow(stringResource(R.string.title_release_gate), gateText(owned.releaseGate) + gateDelay(owned.gateDelayDays)) {
                    viewModel.open(TitleSheet.Gate)
                }
            }
            if (state.isShow) ShowSettingRows(state, viewModel)
            if (state.kindFolders.size > 1) {
                ChoiceRow(
                    stringResource(R.string.title_folder),
                    state.kindFolders.firstOrNull { it.id == owned.libraryFolderId }?.shown ?: state.folderName,
                    state.kindFolders.map { it.id to folderLabel(it, state) },
                    owned.libraryFolderId,
                ) { id -> state.kindFolders.firstOrNull { it.id == id }?.let { viewModel.open(TitleSheet.Move(it)) } }
            }
            if (state.access.subtitlesOn) SubtitlesRow(state, viewModel)
            if (!state.access.handsOff) {
                MenuRow(stringResource(R.string.title_rescan), stringResource(R.string.title_rescan_hint)) { viewModel.openLeftovers() }
            }
            if (state.leftoverCount > 0) {
                MenuRow(
                    stringResource(R.string.title_leftovers),
                    pluralStringResource(R.plurals.title_leftover_count, state.leftoverCount, state.leftoverCount),
                ) { viewModel.openLeftovers() }
            }
            if (state.renameCount > 0) {
                MenuRow(
                    stringResource(R.string.title_rename_files),
                    pluralStringResource(R.plurals.title_rename_count, state.renameCount, state.renameCount),
                ) { viewModel.openRename() }
            }
        }
    }
}

private fun folderLabel(
    folder: ch.orellbuehler.acolyte.api.model.LibraryFolder,
    state: TitleState,
): String {
    val clash = state.kindFolders.count { it.label == folder.label } > 1
    return if (clash) {
        folder.path
            .split('/', '\\')
            .filter { it.isNotEmpty() }
            .takeLast(2)
            .joinToString("/")
    } else {
        folder.shown
    }
}

@Composable
private fun QualityRow(
    state: TitleState,
    viewModel: TitleViewModel,
) {
    val owned = state.owned ?: return
    val presets = state.qualityPresets
    val options = state.kindRules.map { it.id to it.name }
    val current = state.kindRules.firstOrNull { it.id == owned.pickRulesId }
    val hint =
        when (current?.name) {
            "Lite" -> stringResource(R.string.title_quality_lite)
            "Standard" -> stringResource(R.string.title_quality_standard)
            "Best" -> stringResource(R.string.title_quality_best)
            "4K" -> stringResource(R.string.title_quality_4k)
            null -> owned.pickRulesName
            else -> stringResource(R.string.title_quality_custom)
        }
    ChoiceRow(
        stringResource(R.string.title_quality),
        (current?.name ?: owned.pickRulesName) + if (hint.isNotBlank() && hint != current?.name) " · $hint" else "",
        (presets.map { it.id to it.name } + options.filter { o -> presets.none { it.id == o.first } }),
        owned.pickRulesId,
        enabled = state.choicesLoaded && state.busy != "rules",
    ) { id -> viewModel.launch { viewModel.presenter.setRules(id) } }
}

@Composable
private fun ShowSettingRows(
    state: TitleState,
    viewModel: TitleViewModel,
) {
    val show = state.owned?.show ?: return
    val presenter = viewModel.presenter
    val pickLabel =
        when (state.seasonPick) {
            SeasonPick.ALL -> stringResource(R.string.title_seasons_all)
            SeasonPick.LATEST -> stringResource(R.string.title_seasons_latest)
            SeasonPick.OFF -> stringResource(R.string.title_seasons_none)
            else -> stringResource(R.string.title_seasons_custom)
        }
    ChoiceRow(
        stringResource(R.string.title_seasons),
        pickLabel,
        listOf(
            0L to stringResource(R.string.title_seasons_all),
            1L to stringResource(R.string.title_seasons_latest),
            2L to stringResource(R.string.title_seasons_none),
        ),
        when (state.seasonPick) {
            SeasonPick.ALL -> 0L
            SeasonPick.LATEST -> 1L
            SeasonPick.OFF -> 2L
            else -> -1L
        },
        enabled = state.busy != "seasons",
    ) { pick ->
        viewModel.launch {
            presenter.setSeasons(
                when (pick) {
                    0L -> SeasonPick.ALL
                    1L -> SeasonPick.LATEST
                    else -> SeasonPick.OFF
                },
            )
            viewModel.seasons().reset(false)
        }
    }
    val groups = state.episodeGroups?.groups.orEmpty()
    val chosenGroup = show.episodeGroup?.id.orEmpty()
    val orderNames =
        listOf(
            "aired" to stringResource(R.string.title_order_aired),
            "daily" to stringResource(R.string.title_order_daily),
            "absolute" to stringResource(R.string.title_order_absolute),
        )
    val orderLabel =
        if (chosenGroup.isNotEmpty()) {
            show.episodeGroup?.name?.ifBlank { null } ?: stringResource(R.string.title_order_unavailable)
        } else {
            orderNames.firstOrNull { it.first == show.episodeOrder }?.second ?: show.episodeOrder
        }
    val orderOptions =
        orderNames.mapIndexed { i, o -> (-(i + 1)).toLong() to o.second } + groups.mapIndexed { i, g -> i.toLong() to g.name }
    ChoiceRow(
        stringResource(R.string.title_episode_order),
        orderLabel,
        orderOptions,
        if (chosenGroup.isNotEmpty()) {
            groups.indexOfFirst { it.id == chosenGroup }.toLong()
        } else {
            (
                -(
                    orderNames.indexOfFirst {
                        it.first ==
                            show.episodeOrder
                    } +
                        1
                )
            ).toLong()
        },
        enabled = state.busy != "order",
    ) { key ->
        viewModel.launch {
            if (key < 0) presenter.setOrder(orderNames[(-key - 1).toInt()].first) else presenter.setEpisodeGroup(groups[key.toInt()].id)
            viewModel.seasons().reset(
                viewModel.state.value.owned
                    ?.show
                    ?.episodeGroup
                    ?.id
                    ?.isNotEmpty() == true,
            )
        }
    }
    val windows = listOf(0, 1, 2, 3, 5)
    ChoiceRow(
        stringResource(R.string.title_season_packs),
        packText(show.packWindow),
        windows.map { it.toLong() to packText(it) },
        show.packWindow.toLong(),
        hint = stringResource(R.string.title_season_packs_hint),
        enabled = state.busy != "packs",
    ) { n -> viewModel.launch { presenter.setPackWindow(n.toInt()) } }
}

@Composable
private fun packText(window: Int): String =
    when (window) {
        0 -> stringResource(R.string.title_pack_whole)
        else -> pluralStringResource(R.plurals.title_pack_window, window, window)
    }

@Composable
private fun SubtitlesRow(
    state: TitleState,
    viewModel: TitleViewModel,
) {
    val presenter = remember(state.ownedId) { viewModel.subtitles(0) }
    val subs by presenter.state.collectAsStateWithLifecycle()
    LaunchedEffect(presenter) { presenter.load() }
    val value =
        when (subs.mode) {
            SubtitleMode.DEFAULT ->
                stringResource(R.string.title_subtitles_default) +
                    subs.wishCodes
                        .takeIf { it.isNotBlank() }
                        ?.let { " · $it" }
                        .orEmpty()
            SubtitleMode.OFF -> stringResource(R.string.title_subtitles_off)
            SubtitleMode.CUSTOM -> subs.wishCodes
        }
    ChoiceRow(
        stringResource(R.string.title_subtitles),
        value,
        listOf(
            0L to stringResource(R.string.title_subtitles_default),
            1L to stringResource(R.string.title_subtitles_off),
            2L to stringResource(R.string.title_subtitles_choose),
            3L to stringResource(R.string.title_subtitles_search),
        ),
        when (subs.mode) {
            SubtitleMode.DEFAULT -> 0L
            SubtitleMode.OFF -> 1L
            SubtitleMode.CUSTOM -> -1L
        },
    ) { key ->
        when (key) {
            0L -> viewModel.launch { presenter.useDefault() }
            1L -> viewModel.launch { presenter.turnOff() }
            2L -> viewModel.open(TitleSheet.Subtitles(presenter))
            else -> viewModel.launch { presenter.search() }
        }
    }
}

@Composable
fun gateText(gate: String): String =
    when (gate) {
        "announced" -> stringResource(R.string.title_gate_announced)
        "in_cinemas" -> stringResource(R.string.title_gate_in_cinemas)
        else -> stringResource(R.string.title_gate_released)
    }

@Composable
private fun gateDelay(days: Int): String = if (days > 0) " · " + pluralStringResource(R.plurals.title_gate_days, days, days) else ""

@Composable
fun SettingSwitch(
    title: String,
    hint: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.fillMaxWidth().clickable(enabled = enabled) {
                onChange(!checked)
            },
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (!hint.isNullOrBlank()) {
                Text(
                    hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
fun MenuRow(
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = Spacing.Small)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        if (value.isNotBlank()) Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun NoteLine(
    title: String,
    text: String,
) {
    Column(Modifier.padding(vertical = Spacing.XSmall)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.acolyte.waitText)
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ChoiceRow(
    title: String,
    value: String,
    options: List<Pair<Long, String>>,
    selected: Long,
    hint: String? = null,
    enabled: Boolean = true,
    onPick: (Long) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        Column(Modifier.fillMaxWidth().clickable(enabled = enabled) { open = true }.padding(vertical = Spacing.Small)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            if (!hint.isNullOrBlank()) {
                Text(
                    hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (key, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    trailingIcon = { if (key == selected) Icon(Icons.Rounded.Check, contentDescription = null) },
                    onClick = {
                        open = false
                        if (key != selected) onPick(key)
                    },
                )
            }
        }
    }
}
