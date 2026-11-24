package ch.orellbuehler.acolyte.ui.shell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.downloads.DownloadsBadge
import ch.orellbuehler.acolyte.feature.downloads.DownloadsBadgeKind
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.SectionHeader
import ch.orellbuehler.acolyte.ui.nav.destination
import ch.orellbuehler.acolyte.ui.nav.title
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

@Composable
fun MoreScreen(
    account: Account,
    onOpen: (Section) -> Unit,
    onSignOut: () -> Unit,
    onOpenDecisions: () -> Unit = {},
) {
    val badges = LocalSdk.current.downloadBadges
    val badge by badges.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(badges, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { badges.watch() } }
    ScreenScaffold(title = stringResource(account.moreTab.destination.label)) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            if (badge.decide > 0 && account.sections.contains(Section.DOWNLOADS)) {
                item(key = "decide-card") { DecideCard(badge.decide, onOpenDecisions) }
            }
            account.moreGroups.forEach { group ->
                group.group.title?.let { title -> item(key = group.group.name) { SectionHeader(stringResource(title)) } }
                items(group.sections, key = { it.name }) { section ->
                    SectionRow(account, section, if (section == Section.DOWNLOADS) badge else null, onOpen)
                }
            }
            item(key = "sign-out") {
                OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().padding(Spacing.Screen)) {
                    Text(stringResource(R.string.account_sign_out), color = MaterialTheme.acolyte.failText)
                }
            }
        }
    }
}

@Composable
private fun SectionRow(
    account: Account,
    section: Section,
    badge: DownloadsBadge?,
    onOpen: (Section) -> Unit,
) {
    val inbox by LocalSdk.current.inbox.state
        .collectAsStateWithLifecycle()
    val destination = section.destination
    ListItem(
        headlineContent = { Text(stringResource(destination.label)) },
        supportingContent =
            if (section == Section.ACCOUNT) {
                { Text(stringResource(R.string.account_summary, account.displayName, account.serverName)) }
            } else {
                null
            },
        leadingContent = { Icon(destination.icon, contentDescription = null) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val requestsBadge = if (section == Section.REQUESTS) inbox.requestsBadge else 0
                if (requestsBadge > 0) Badge { Text(requestsBadge.toString()) }
                if (badge != null) DownloadsBadgeLabel(badge)
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.clickable { onOpen(section) },
    )
}

@Composable
private fun DownloadsBadgeLabel(badge: DownloadsBadge) {
    when (badge.kind) {
        DownloadsBadgeKind.ACTIVE ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                CircularProgressIndicator(
                    progress = { badge.percent / 100f },
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    trackColor = MaterialTheme.colorScheme.outline,
                )
                Text(
                    stringResource(R.string.downloads_badge_active, badge.active, badge.percent),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        DownloadsBadgeKind.DECIDE ->
            Text(
                stringResource(R.string.downloads_badge_decide, badge.decide),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.acolyte.failText,
            )
        DownloadsBadgeKind.NONE -> Unit
    }
}

@Composable
private fun DecideCard(
    count: Int,
    onOpen: () -> Unit,
) {
    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
    ) {
        Row(Modifier.padding(Spacing.Large), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.downloads_badge_decide, count),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.acolyte.failText,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
