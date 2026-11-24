package ch.orellbuehler.acolyte.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.settings.SettingsCatalog
import ch.orellbuehler.acolyte.feature.settings.SettingsGroup
import ch.orellbuehler.acolyte.feature.settings.SettingsHomeGroup
import ch.orellbuehler.acolyte.feature.settings.SettingsHomePage
import ch.orellbuehler.acolyte.feature.settings.SettingsHomeReason
import ch.orellbuehler.acolyte.feature.settings.SettingsHomeReasonKind
import ch.orellbuehler.acolyte.feature.settings.SettingsHomeStanding
import ch.orellbuehler.acolyte.feature.settings.SettingsPage
import ch.orellbuehler.acolyte.feature.settings.SettingsPageText
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.MessageView
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.nav.RequestsRoute
import ch.orellbuehler.acolyte.ui.nav.Routes
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

@Composable
fun SettingsHomeScreen(
    account: Account,
    onBack: (() -> Unit)?,
    onOpen: (Any) -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { SettingsHomeViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.watch() } }
    var query by rememberSaveable { mutableStateOf("") }
    val texts =
        state.pages.map {
            SettingsPageText(it.key, stringResource(it.labelRes()), stringResource(it.aboutRes()), stringResource(it.group.titleRes()))
        }
    val found = remember(query, texts) { SettingsCatalog.search(query, texts) }
    val uri = LocalUriHandler.current
    ScreenScaffold(title = stringResource(R.string.section_settings), onBack = onBack) { padding ->
        if (!state.allowed) {
            MessageView(
                icon = Icons.Outlined.Lock,
                title = stringResource(R.string.settings_home_not_allowed),
                message = stringResource(R.string.settings_home_not_allowed_text),
                modifier = Modifier.padding(padding),
            )
            return@ScreenScaffold
        }
        StatusContent(status = state.status, isEmpty = false, onRefresh = viewModel::refresh, modifier = Modifier.padding(padding)) {
            LazyColumn {
                item(key = "search") { SearchField(query, { query = it }) }
                if (query.isBlank()) {
                    if (state.showFinishSetup) {
                        item(key = "finish") { FinishSetupCard { uri.openUri(account.serverUrl.trimEnd('/') + "/welcome?first=1") } }
                    }
                    if (state.reasons.isNotEmpty()) {
                        item(key = "attention") { AttentionCard(state.reasons, onOpen) }
                    }
                    state.groups.forEach { group ->
                        item(key = group.group.name) { GroupHeader(group) }
                        items(group.pages, key = { it.page.name }) { SettingsPageRow(it) { page -> onOpen(page.route()) } }
                    }
                    if (state.accountPages.isNotEmpty()) {
                        item(key = SettingsGroup.ACCOUNT.name) { GroupHeader(SettingsHomeGroup(SettingsGroup.ACCOUNT, emptyList())) }
                        items(
                            state.accountPages,
                            key = { it.name },
                        ) { SettingsPageRow(SettingsHomePage(it)) { page -> onOpen(page.route()) } }
                    }
                } else if (found.isEmpty()) {
                    item(key = "empty") { NoResults { query = "" } }
                } else {
                    items(found, key = { it }) { key ->
                        val page = state.pages.firstOrNull { it.key == key }
                        if (page != null) SearchRow(page) { onOpen(page.route()) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        placeholder = { Text(stringResource(R.string.settings_home_search)) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.settings_home_search_clear))
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
    )
}

@Composable
private fun FinishSetupCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
    ) {
        Row(
            Modifier.padding(Spacing.Large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Icon(Icons.Outlined.Tune, contentDescription = null)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_home_finish_setup), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.settings_home_finish_setup_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Chevron()
        }
    }
}

@Composable
private fun AttentionCard(
    reasons: List<SettingsHomeReason>,
    onOpen: (Any) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
    ) {
        Column(Modifier.padding(vertical = Spacing.Small)) {
            Text(
                pluralStringResource(R.plurals.settings_home_attention_count, reasons.size, reasons.size),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = Spacing.Large, vertical = Spacing.Small),
            )
            reasons.forEach { reason -> ReasonRow(reason, onOpen) }
        }
    }
}

@Composable
private fun ReasonRow(
    reason: SettingsHomeReason,
    onOpen: (Any) -> Unit,
) {
    val colors = MaterialTheme.acolyte
    val title =
        when (reason.kind) {
            SettingsHomeReasonKind.SETTING -> reason.title
            SettingsHomeReasonKind.UPDATE -> stringResource(R.string.settings_home_update_title)
            SettingsHomeReasonKind.DECISIONS -> stringResource(R.string.settings_home_decisions, reason.count)
            SettingsHomeReasonKind.REQUESTS -> stringResource(R.string.settings_home_requests, reason.count)
        }
    val body =
        when (reason.kind) {
            SettingsHomeReasonKind.SETTING -> reason.body
            SettingsHomeReasonKind.UPDATE -> stringResource(R.string.settings_home_update_body, reason.body)
            else -> ""
        }
    val target: Any? =
        when (reason.kind) {
            SettingsHomeReasonKind.DECISIONS -> Routes.downloads(show = "problems.decide")
            SettingsHomeReasonKind.REQUESTS -> RequestsRoute
            else -> reason.page?.route()
        }
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (body.isNotEmpty()) ({ Text(body) }) else null,
        leadingContent = { Box(Modifier.size(10.dp).background(colors.dot(reason.tone), CircleShape)) },
        trailingContent = if (target != null) ({ Chevron() }) else null,
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = if (target != null) Modifier.clickable { onOpen(target) } else Modifier,
    )
}

@Composable
private fun GroupHeader(group: SettingsHomeGroup) {
    Column(Modifier.padding(start = Spacing.Screen, end = Spacing.Screen, top = Spacing.Large, bottom = Spacing.XSmall)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Text(stringResource(group.group.titleRes()), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (group.standing == SettingsHomeStanding.ATTENTION) {
                Text(
                    pluralStringResource(R.plurals.settings_home_attention_count, group.count, group.count),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.acolyte.failText,
                )
            } else if (group.standing == SettingsHomeStanding.UNSET) {
                Text(
                    stringResource(R.string.settings_home_unset),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.acolyte.waitText,
                )
            }
        }
        Text(
            stringResource(group.group.lineRes()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsPageRow(
    entry: SettingsHomePage,
    onClick: (SettingsPage) -> Unit,
) {
    val colors = MaterialTheme.acolyte
    ListItem(
        headlineContent = { Text(stringResource(entry.page.labelRes())) },
        supportingContent = { Text(stringResource(entry.page.aboutRes())) },
        leadingContent = { PageIcon(entry.page) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                when (entry.standing) {
                    SettingsHomeStanding.ATTENTION -> {
                        Box(Modifier.size(8.dp).background(colors.fail, CircleShape))
                        Text(
                            pluralStringResource(R.plurals.settings_home_attention_count, entry.count, entry.count),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.failText,
                        )
                    }
                    SettingsHomeStanding.UNSET ->
                        Text(
                            stringResource(R.string.settings_home_unset),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.waitText,
                        )
                    else -> Unit
                }
                Chevron()
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.clickable { onClick(entry.page) },
    )
}

@Composable
private fun SearchRow(
    page: SettingsPage,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(page.labelRes())) },
        supportingContent = { Text(stringResource(page.group.titleRes())) },
        leadingContent = { PageIcon(page) },
        trailingContent = { Chevron() },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun PageIcon(page: SettingsPage) {
    Box(
        Modifier.size(36.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(page.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun Chevron() {
    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun NoResults(onClear: () -> Unit) {
    MessageView(
        icon = Icons.Rounded.Search,
        title = stringResource(R.string.settings_home_search_empty_title),
        message = stringResource(R.string.settings_home_search_empty_text),
        action = stringResource(R.string.settings_home_search_clear),
        onAction = onClear,
        modifier = Modifier.padding(top = Spacing.XLarge),
    )
}
