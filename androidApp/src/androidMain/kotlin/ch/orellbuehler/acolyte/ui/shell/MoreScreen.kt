package ch.orellbuehler.acolyte.ui.shell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.session.Account
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
) {
    ScreenScaffold(title = stringResource(account.moreTab.destination.label)) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            account.moreGroups.forEach { group ->
                group.group.title?.let { title -> item(key = group.group.name) { SectionHeader(stringResource(title)) } }
                items(group.sections, key = { it.name }) { section -> SectionRow(account, section, onOpen) }
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
    onOpen: (Section) -> Unit,
) {
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
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.clickable { onOpen(section) },
    )
}
