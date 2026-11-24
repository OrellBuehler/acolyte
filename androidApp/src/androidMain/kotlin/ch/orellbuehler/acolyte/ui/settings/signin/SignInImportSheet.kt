package ch.orellbuehler.acolyte.ui.settings.signin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInCandidate
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInImport
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInImportPhase
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInWayKind
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.requests.ConfirmDialog
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImportSheet(
    sheet: SettingsSignInImport,
    model: SignInModel,
) {
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val plex = sheet.kind == SettingsSignInWayKind.PLEX
    ModalBottomSheet(
        onDismissRequest = model.presenter::closeImport,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.padding(horizontal = Spacing.Screen).navigationBarsPadding().padding(bottom = Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Text(stringResource(R.string.settings_signin_import_from, sheet.kind.app), style = MaterialTheme.typography.titleLarge)
            when (sheet.phase) {
                SettingsSignInImportPhase.ASK ->
                    if (plex) {
                        PlexRetry(sheet, model)
                    } else {
                        AdminForm(sheet, name, password, { name = it }, { password = it }) {
                            val typedName = name
                            val typedPassword = password
                            password = ""
                            model.run { previewImport(typedName, typedPassword) }
                        }
                    }
                SettingsSignInImportPhase.LOADING ->
                    Box(Modifier.fillMaxWidth().padding(Spacing.XLarge), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                SettingsSignInImportPhase.READY -> PeopleList(sheet, model)
            }
            TextButton(onClick = model.presenter::closeImport, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_signin_cancel))
            }
        }
    }
    if (sheet.confirmAdults.isNotEmpty()) {
        ConfirmDialog(
            title = stringResource(R.string.settings_signin_adult_title),
            text = stringResource(R.string.settings_signin_adult_text, sheet.confirmAdults.joinToString(", ")),
            confirm = stringResource(R.string.settings_signin_adult_confirm),
            dismiss = stringResource(R.string.settings_signin_cancel),
            onConfirm = { model.run { runImport(confirmAdults = true) } },
            onDismiss = model.presenter::dismissConfirmAdults,
        )
    }
}

@Composable
private fun PlexRetry(
    sheet: SettingsSignInImport,
    model: SignInModel,
) {
    sheet.error?.let { FailText(failureText(it)) }
    OutlinedButton(onClick = { model.run { previewImport("", "") } }) { Text(stringResource(R.string.settings_signin_try_again)) }
}

@Composable
private fun AdminForm(
    sheet: SettingsSignInImport,
    name: String,
    password: String,
    onName: (String) -> Unit,
    onPassword: (String) -> Unit,
    onShow: () -> Unit,
) {
    Hint(stringResource(R.string.settings_signin_import_intro, sheet.kind.app))
    SignInField(stringResource(R.string.settings_signin_admin_name), name, onName)
    SignInField(stringResource(R.string.settings_signin_admin_pass), password, onPassword, password = true)
    sheet.error?.let { FailText(failureText(it)) }
    Button(onClick = onShow, enabled = name.isNotBlank()) { Text(stringResource(R.string.settings_signin_show_people)) }
}

@Composable
private fun PeopleList(
    sheet: SettingsSignInImport,
    model: SignInModel,
) {
    if (sheet.people.isEmpty()) {
        Hint(stringResource(R.string.settings_signin_no_one))
        return
    }
    Hint(stringResource(R.string.settings_signin_import_note))
    Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
        sheet.people.forEachIndexed { index, person ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            CandidateRow(person, model)
        }
    }
    sheet.error?.let { FailText(failureText(it)) }
    Button(
        onClick = { model.run { runImport() } },
        enabled = sheet.pickedCount > 0 && !sheet.busy,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (sheet.busy) {
            SmallSpinner(Modifier.padding(end = Spacing.Small))
        }
        Text(stringResource(R.string.settings_signin_import_n, sheet.pickedCount))
    }
}

@Composable
private fun CandidateRow(
    person: SettingsSignInCandidate,
    model: SignInModel,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(
                enabled = !person.locked,
            ) { model.presenter.togglePerson(person.id) }
            .padding(vertical = Spacing.XSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = person.picked, onCheckedChange = { model.presenter.togglePerson(person.id) }, enabled = !person.locked)
        Column(Modifier.weight(1f)) {
            Text(person.name, style = MaterialTheme.typography.bodyLarge)
            val detail =
                listOfNotNull(
                    person.email.takeIf { it.isNotEmpty() },
                    person.linkedTo.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.settings_signin_already, it) },
                ).joinToString(" · ")
            if (detail.isNotEmpty()) Hint(detail)
            if (person.serverAdmin) StatusChip(stringResource(R.string.settings_signin_server_admin), Tone.IDLE)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = person.asKid,
                onCheckedChange = { model.presenter.toggleKid(person.id) },
                enabled = !person.locked && person.picked,
            )
            Text(stringResource(R.string.settings_signin_kid), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
