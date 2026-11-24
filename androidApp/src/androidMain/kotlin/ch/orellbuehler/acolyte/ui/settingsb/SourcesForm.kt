package ch.orellbuehler.acolyte.ui.settingsb

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceAddressHint
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceForm
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceKeyKind
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceProblem
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceSites
import ch.orellbuehler.acolyte.feature.sources.SettingsSourcesState
import ch.orellbuehler.acolyte.ui.components.NoticeBanner
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.PickerField
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun SourceFormCard(
    initial: SettingsSourceForm,
    state: SettingsSourcesState,
    model: SourcesViewModel,
) {
    var form by remember { mutableStateOf(initial) }
    var tagText by remember { mutableStateOf(initial.tags.joinToString(", ")) }
    var submitted by remember { mutableStateOf(false) }
    var limitsOpen by remember { mutableStateOf(false) }
    var proxyOpen by remember { mutableStateOf(initial.proxy.isNotEmpty()) }

    fun update(next: SettingsSourceForm) {
        form = model.settled(next)
    }

    val problems = form.problems().toSet()

    @Composable
    fun problemFor(
        problem: SettingsSourceProblem,
        shown: Boolean,
    ): String? = if (problem in problems && (submitted || shown)) sourceProblemText(problem) else null

    @Composable
    fun field(
        label: String,
        value: String,
        onChange: (String) -> Unit,
        modifier: Modifier = Modifier,
        hint: String? = null,
        error: String? = null,
        password: Boolean = false,
        keyboard: KeyboardType = KeyboardType.Text,
        placeholder: String? = null,
        singleLine: Boolean = true,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            supportingText = (error ?: hint)?.let { { Text(it) } },
            isError = error != null,
            singleLine = singleLine,
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboard),
            modifier = modifier.fillMaxWidth(),
        )
    }

    SettingsCard(
        title =
            if (form.isNew) {
                stringResource(R.string.acq_src_add)
            } else {
                stringResource(R.string.acq_src_change_title, form.name.ifBlank { initial.name })
            },
    ) {
        field(
            stringResource(R.string.acq_src_name),
            form.name,
            { update(form.copy(name = it.take(SettingsSourceForm.NAME_MAX + 20))) },
            error = problemFor(SettingsSourceProblem.NAME, form.name.isNotBlank()),
        )
        val formats =
            buildList {
                add(SettingsSourceForm.FORMAT_TORZNAB to stringResource(R.string.acq_src_fmt_torznab))
                add(SettingsSourceForm.FORMAT_NEWZNAB to stringResource(R.string.acq_src_fmt_newznab))
                add(SettingsSourceForm.FORMAT_RSS to stringResource(R.string.acq_src_fmt_rss))
                if (form.showSiteFormat) add(SettingsSourceForm.FORMAT_SITE to stringResource(R.string.acq_src_fmt_site))
            }
        PickerField(
            stringResource(R.string.acq_src_format),
            formats,
            form.format,
            { update(form.withFormat(it)) },
            enabled = !form.formatLocked,
        )
        if (form.isSite) {
            PickerField(
                stringResource(R.string.acq_src_site),
                SettingsSourceSites.all.map { it.id to it.name },
                form.site,
                { update(form.withSite(it)) },
                enabled = !form.siteLocked,
            )
        } else {
            PickerField(
                stringResource(R.string.acq_src_downloads),
                listOf(
                    SettingsSourceForm.PROTOCOL_TORRENT to stringResource(R.string.acq_src_downloads_torrent),
                    SettingsSourceForm.PROTOCOL_USENET to stringResource(R.string.acq_src_downloads_usenet),
                ),
                form.protocolName,
                { update(form.copy(protocolName = it)) },
            )
        }
        val urlError =
            problemFor(SettingsSourceProblem.URL, form.url.isNotBlank())
                ?: problemFor(SettingsSourceProblem.URL_QUERY, form.url.isNotBlank())
        field(
            stringResource(R.string.acq_src_address),
            form.url,
            { update(form.copy(url = it)) },
            hint =
                stringResource(
                    when (form.addressHint) {
                        SettingsSourceAddressHint.RSS -> R.string.acq_src_address_rss
                        SettingsSourceAddressHint.SITE -> R.string.acq_src_address_site
                        SettingsSourceAddressHint.FEED -> R.string.acq_src_address_feed
                    },
                ),
            error = urlError,
            keyboard = KeyboardType.Uri,
            placeholder = if (form.isRss) "https://example.org/rss?passkey=…" else "https://example.org/api",
        )
        if (form.showLoginChoice) {
            PickerField(
                stringResource(R.string.acq_src_login),
                listOf(
                    SettingsSourceForm.LOGIN_PASSKEY to stringResource(R.string.acq_src_login_passkey),
                    SettingsSourceForm.LOGIN_COOKIE to stringResource(R.string.acq_src_login_cookie),
                ),
                form.siteLogin,
                { update(form.copy(siteLogin = it)) },
            )
        }
        when (form.keyKind) {
            SettingsSourceKeyKind.COOKIE ->
                field(
                    stringResource(R.string.acq_src_cookie),
                    form.apiKey,
                    { update(form.copy(apiKey = it)) },
                    hint = stringResource(R.string.acq_src_cookie_hint),
                    password = true,
                )
            SettingsSourceKeyKind.PASSKEY ->
                field(
                    stringResource(R.string.acq_src_passkey),
                    form.apiKey,
                    { update(form.copy(apiKey = it)) },
                    hint = stringResource(R.string.acq_src_passkey_hint),
                    password = true,
                )
            SettingsSourceKeyKind.API_KEY ->
                field(
                    stringResource(R.string.acq_src_api_key),
                    form.apiKey,
                    { update(form.copy(apiKey = it)) },
                    hint = stringResource(R.string.acq_src_api_key_hint),
                    password = true,
                )
            SettingsSourceKeyKind.NONE -> Unit
        }
        if (form.showAgent) {
            field(
                stringResource(R.string.acq_src_agent),
                form.siteAgent,
                { update(form.copy(siteAgent = it)) },
                hint = stringResource(R.string.acq_src_agent_hint),
                error = problemFor(SettingsSourceProblem.SITE_AGENT, true),
            )
        }
        if (form.showUser) {
            field(
                stringResource(R.string.acq_src_user),
                form.siteUser,
                { update(form.copy(siteUser = it)) },
                hint = stringResource(R.string.acq_src_user_hint),
                error = problemFor(SettingsSourceProblem.SITE_USER, true),
                keyboard = KeyboardType.Number,
            )
        }
        Text(stringResource(R.string.acq_src_use_for), style = MaterialTheme.typography.titleSmall)
        CheckLine(stringResource(R.string.acq_src_newest), form.feed) { update(form.copy(feed = it)) }
        if (form.feed && !form.newestListWorks) Hint(stringResource(R.string.acq_src_no_newest))
        if (form.showSearchUse) {
            CheckLine(stringResource(R.string.acq_src_auto), form.autoSearch) { update(form.copy(autoSearch = it)) }
            CheckLine(stringResource(R.string.acq_src_hand), form.handSearch) { update(form.copy(handSearch = it)) }
        }
        Text(stringResource(R.string.acq_src_media), style = MaterialTheme.typography.titleSmall)
        listOf(
            "movie" to R.string.acq_src_movies,
            "tv" to R.string.acq_src_tv,
            "music" to R.string.acq_src_music,
        ).forEach { (kind, label) ->
            CheckLine(stringResource(label), kind in form.kinds) { update(form.withKind(kind, it)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            field(
                stringResource(R.string.acq_src_priority),
                form.priority,
                { update(form.copy(priority = it.filter { c -> c.isDigit() }.take(3))) },
                Modifier.weight(1f),
                hint = stringResource(R.string.acq_src_priority_hint),
                error = problemFor(SettingsSourceProblem.PRIORITY, true),
                keyboard = KeyboardType.Number,
            )
            if (form.showSeeders) {
                field(
                    stringResource(R.string.acq_src_seeders),
                    form.minSeeders,
                    { update(form.copy(minSeeders = it.filter { c -> c.isDigit() }.take(6))) },
                    Modifier.weight(1f),
                    error = problemFor(SettingsSourceProblem.SEEDERS, true),
                    keyboard = KeyboardType.Number,
                )
            }
        }
        field(
            stringResource(R.string.acq_src_tags),
            tagText,
            {
                tagText = it
                update(form.withTagText(it))
            },
            hint = stringResource(R.string.acq_src_tags_hint),
        )
        form.sendTo(state.downloaders)?.let { choices ->
            PickerField(
                stringResource(R.string.acq_src_send_to),
                listOf(0L to stringResource(R.string.acq_src_send_to_any)) + choices.map { it.id to it.name },
                form.downloaderId,
                { update(form.copy(downloaderId = it)) },
                hint = stringResource(R.string.acq_src_send_to_hint),
            )
            if (form.sendGone) NoticeBanner(stringResource(R.string.acq_src_send_to_gone))
        }
        field(
            stringResource(R.string.acq_src_multi),
            form.multiLanguages,
            { update(form.copy(multiLanguages = it)) },
            hint = stringResource(R.string.acq_src_multi_hint),
            placeholder = "fr, en",
        )
        field(
            stringResource(R.string.acq_src_no_language),
            form.noLanguageLanguages,
            { update(form.copy(noLanguageLanguages = it)) },
            hint = stringResource(R.string.acq_src_no_language_hint),
            error = problemFor(SettingsSourceProblem.LANGUAGES, true),
            placeholder = "uk",
        )
        Details(stringResource(R.string.acq_src_limits), limitsOpen, { limitsOpen = it }) {
            field(
                stringResource(R.string.acq_src_cap),
                form.searchesPerWindow,
                { update(form.copy(searchesPerWindow = it.filter { c -> c.isDigit() }.take(6))) },
                hint = stringResource(R.string.acq_src_cap_hint),
                error = problemFor(SettingsSourceProblem.SEARCHES, true),
                keyboard = KeyboardType.Number,
            )
            field(
                stringResource(R.string.acq_src_window),
                form.windowHours,
                { update(form.copy(windowHours = it.take(8))) },
                error = problemFor(SettingsSourceProblem.WINDOW, true),
                keyboard = KeyboardType.Decimal,
            )
            field(
                stringResource(R.string.acq_src_gap),
                form.gapMs,
                { update(form.copy(gapMs = it.filter { c -> c.isDigit() }.take(7))) },
                error = problemFor(SettingsSourceProblem.GAP, true),
                keyboard = KeyboardType.Number,
            )
            field(
                stringResource(R.string.acq_src_timeout),
                form.timeoutSeconds,
                { update(form.copy(timeoutSeconds = it.take(8))) },
                error = problemFor(SettingsSourceProblem.TIMEOUT, true),
                keyboard = KeyboardType.Decimal,
            )
        }
        Details(stringResource(R.string.acq_src_proxy), proxyOpen, { proxyOpen = it }) {
            field(
                stringResource(R.string.acq_src_proxy_address),
                form.proxy,
                { update(form.copy(proxy = it)) },
                error = problemFor(SettingsSourceProblem.PROXY, true),
                keyboard = KeyboardType.Uri,
                placeholder = stringResource(R.string.acq_src_proxy_example),
            )
            field(
                stringResource(R.string.acq_src_proxy_password),
                form.proxyPassword,
                { update(form.copy(proxyPassword = it)) },
                password = true,
            )
            CheckLine(stringResource(R.string.acq_src_proxy_local), form.proxyAllowLocalNetwork) {
                update(form.copy(proxyAllowLocalNetwork = it))
            }
        }
        CheckLine(stringResource(R.string.acq_src_local), form.allowLocalNetwork, hint = stringResource(R.string.acq_src_local_hint)) {
            update(form.copy(allowLocalNetwork = it))
        }
        state.saveError?.let { FailText(sourceErrorText(it)) }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    submitted = true
                    if (form.canSave) model.save(form)
                },
                enabled = !state.saving && form.name.isNotBlank() && form.url.isNotBlank(),
            ) {
                Text(stringResource(R.string.acq_src_save))
            }
            OutlinedButton(onClick = model::cancelEdit, enabled = !state.saving) { Text(stringResource(R.string.acq_src_cancel)) }
        }
    }
}

@Composable
fun CheckLine(
    label: String,
    checked: Boolean,
    hint: String? = null,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (hint != null) Hint(hint)
        }
    }
}

@Composable
private fun Details(
    title: String,
    open: Boolean,
    onToggle: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable { onToggle(!open) }.padding(vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
    }
    if (open) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) { content() }
    }
}
