package ch.orellbuehler.acolyte.ui.settings.signin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInLineProblem
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInLocal
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInPerson
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInSeen
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInSkipOff
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInState
import ch.orellbuehler.acolyte.image.ImageSize
import ch.orellbuehler.acolyte.ui.components.ServerImage
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.requests.SwitchRow
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

@Composable
internal fun LocalCard(
    state: SettingsSignInState,
    model: SignInModel,
) {
    val local = state.local
    val owner = state.isOwner
    SettingsCard(stringResource(R.string.settings_signin_local)) {
        SwitchRow(
            title = stringResource(R.string.settings_signin_skip_local),
            checked = local.skip,
            onChange = { model.run { setSkip(it) } },
            hint = stringResource(R.string.settings_signin_skip_home),
            enabled = owner && !local.skipSaving,
        )
        if (local.mediaApps.isNotEmpty()) Hint(stringResource(R.string.settings_signin_media_on, local.mediaApps.joinToString(", ")))
        if (local.skip &&
            local.skipOff == SettingsSignInSkipOff.PERSON_GONE
        ) {
            InlineNotice(stringResource(R.string.settings_signin_off_gone))
        }
        if (local.skip && local.skipOff == SettingsSignInSkipOff.KID) InlineNotice(stringResource(R.string.settings_signin_off_kid))
        BridgeNotice(local)
        if (local.proxyUntrusted) Hint(stringResource(R.string.settings_signin_proxy_untrusted))
        if (owner) WhoSection(local, model)
        NetworksSection(local, owner, model)
        ProxiesSection(local, state, model)
        if (owner) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                Button(
                    onClick = { model.run { saveLocal() } },
                    enabled = local.canSave,
                ) { Text(stringResource(R.string.settings_signin_save)) }
                if (local.saving) SmallSpinner()
                if (local.problemCount > 0) {
                    Text(
                        pluralStringResource(R.plurals.settings_signin_fix_to_save, local.problemCount, local.problemCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            local.saveError?.let { FailText(failureText(it)) }
        } else {
            Hint(stringResource(R.string.settings_signin_owner_only))
        }
    }
}

@Composable
private fun BridgeNotice(local: SettingsSignInLocal) {
    var how by rememberSaveable { mutableStateOf(false) }
    when (local.seen) {
        SettingsSignInSeen.OWN_GATEWAY ->
            InlineNotice(
                stringResource(R.string.settings_signin_bridge_ok_body),
                title = stringResource(R.string.settings_signin_bridge_ok_title, local.seenAddress),
                warn = false,
            )
        SettingsSignInSeen.BRIDGE ->
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                InlineNotice(
                    stringResource(R.string.settings_signin_bridge_body),
                    title = stringResource(R.string.settings_signin_bridge_title, local.seenAddress),
                )
                TextButton(onClick = { how = !how }) { Text(stringResource(R.string.settings_signin_how_to_fix)) }
                if (how) Hint(stringResource(R.string.settings_signin_how_to_fix_body))
            }
        else -> Unit
    }
}

@Composable
private fun WhoSection(
    local: SettingsSignInLocal,
    model: SignInModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Text(stringResource(R.string.settings_signin_who), style = MaterialTheme.typography.titleSmall)
        val people = local.people
        val error = local.peopleError
        when {
            error != null -> {
                FailText(failureText(error))
                OutlinedButton(onClick = { model.run { retryPeople() } }) { Text(stringResource(R.string.settings_signin_try_again)) }
            }
            people == null -> SmallSpinner()
            else -> people.forEach { person -> PersonTile(person, person.id == local.pickedId) { model.presenter.pickPerson(person.id) } }
        }
        Hint(stringResource(R.string.settings_signin_who_hint))
    }
}

@Composable
private fun PersonTile(
    person: SettingsSignInPerson,
    picked: Boolean,
    onPick: () -> Unit,
) {
    val border = if (picked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Surface(
        onClick = onPick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(if (picked) 2.dp else 1.dp, border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            ServerImage(
                source = person.avatar.ifBlank { null },
                contentDescription = null,
                size = ImageSize.THUMB,
                placeholderName = person.name,
                modifier = Modifier.size(36.dp).clip(CircleShape),
            )
            Column(Modifier.weight(1f)) {
                Text(person.name, style = MaterialTheme.typography.bodyLarge)
                Text(roleLine(person), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (picked) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun NetworksSection(
    local: SettingsSignInLocal,
    owner: Boolean,
    model: SignInModel,
) {
    val presenter = model.presenter
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        SignInArea(
            label = stringResource(R.string.settings_signin_home_nets),
            value = local.networks,
            onValueChange = presenter::setLocalNetworks,
            enabled = owner,
            hint = stringResource(R.string.settings_signin_home_nets_hint),
            minLines = 4,
            isError = local.netProblems.isNotEmpty(),
        )
        Problems(local.netProblems)
        SeenLine(local)
        if (owner) {
            OutlinedButton(onClick = presenter::addHomeRanges) { Text(stringResource(R.string.settings_signin_add_home)) }
            Row(
                Modifier.fillMaxWidth().clickable { presenter.setTailscale(!local.hasTailscale) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = local.hasTailscale, onCheckedChange = presenter::setTailscale)
                Column {
                    Text(stringResource(R.string.settings_signin_cgnat), style = MaterialTheme.typography.bodyMedium)
                    Hint(stringResource(R.string.settings_signin_cgnat_hint, local.cgnatRange))
                }
            }
        }
    }
}

@Composable
private fun SeenLine(local: SettingsSignInLocal) {
    val text =
        when (local.seen) {
            SettingsSignInSeen.NONE -> return
            SettingsSignInSeen.OWN_GATEWAY -> stringResource(R.string.settings_signin_you_bridge_ok, local.seenAddress)
            SettingsSignInSeen.BRIDGE -> stringResource(R.string.settings_signin_you_bridge, local.seenAddress)
            SettingsSignInSeen.COVERED -> stringResource(R.string.settings_signin_you_covered, local.seenAddress)
            SettingsSignInSeen.NOT_COVERED -> stringResource(R.string.settings_signin_you_not, local.seenAddress)
        }
    val good = local.seen == SettingsSignInSeen.OWN_GATEWAY || local.seen == SettingsSignInSeen.COVERED
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Surface(
            shape = CircleShape,
            color = if (good) MaterialTheme.acolyte.ok else MaterialTheme.acolyte.wait,
            modifier = Modifier.size(8.dp),
        ) {}
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ProxiesSection(
    local: SettingsSignInLocal,
    state: SettingsSignInState,
    model: SignInModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Text(stringResource(R.string.settings_signin_proxies), style = MaterialTheme.typography.titleSmall)
        local.fixedProxies.forEach { address ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
                Text(address, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                Hint("· " + stringResource(R.string.settings_signin_fixed_row))
            }
        }
        SignInArea(
            label = stringResource(R.string.settings_signin_proxies),
            value = local.proxies,
            onValueChange = model.presenter::setLocalProxies,
            enabled = state.isOwner && !state.apiKey,
            hint = stringResource(R.string.settings_signin_proxies_help),
            placeholder = "10.0.0.5",
            minLines = 2,
            isError = local.proxyProblems.isNotEmpty(),
        )
        Problems(local.proxyProblems)
        if (state.apiKey) Hint(stringResource(R.string.settings_signin_proxies_needs_session))
    }
}

@Composable
private fun Problems(problems: List<SettingsSignInLineProblem>) {
    if (problems.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        FailText(pluralStringResource(R.plurals.settings_signin_lines_fix, problems.size, problems.size))
        problems.forEach { problem ->
            Column(Modifier.padding(start = Spacing.Small)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    Hint(stringResource(R.string.settings_signin_line_n, problem.line))
                    SelectionContainer {
                        Text(problem.text, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }
                }
                Text(fixText(problem), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
