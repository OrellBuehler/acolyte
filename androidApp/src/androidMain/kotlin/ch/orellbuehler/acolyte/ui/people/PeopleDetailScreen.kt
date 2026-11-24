package ch.orellbuehler.acolyte.ui.people

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.model.TitleUser
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.account.PeopleNotice
import ch.orellbuehler.acolyte.feature.account.PeoplePreset
import ch.orellbuehler.acolyte.feature.account.PeopleProblem
import ch.orellbuehler.acolyte.feature.account.peoplePreset
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.account.FormCard
import ch.orellbuehler.acolyte.ui.account.FormColumn
import ch.orellbuehler.acolyte.ui.account.PaddingValuesZero
import ch.orellbuehler.acolyte.ui.account.ToastOnce
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.PasswordField
import ch.orellbuehler.acolyte.ui.components.ReauthSheet
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.nav.PeopleDevicesRoute
import ch.orellbuehler.acolyte.ui.theme.Spacing

private enum class Dialog {
    NONE,
    CHANGE,
    PASSWORD,
    TURN_OFF,
    RESET,
    DELETE,
    SERVER,
}

@Composable
fun PeopleDetailScreen(
    id: Long,
    account: Account,
    onOpen: (Any) -> Unit,
    onBack: () -> Unit,
) {
    val sdk = LocalSdk.current
    val vm = viewModel(key = "people-detail") { PeopleViewModel(sdk) }
    val state by vm.state.collectAsStateWithLifecycle()
    var dialog by rememberSaveable { mutableStateOf(Dialog.NONE) }
    val user = state.users.firstOrNull { it.id == id }
    LaunchedEffect(state.notice) {
        if (state.notice == PeopleNotice.DELETED) onBack()
    }
    ToastOnce(noticeText(state.notice, state.noticeName).takeIf { state.notice != PeopleNotice.DELETED }) { vm.consume() }
    ScreenScaffold(title = user?.name.orEmpty(), onBack = onBack) { padding ->
        StatusContent(
            state.status,
            isEmpty = user == null && state.status.phase == Phase.READY,
            onRefresh = { vm.refresh() },
            modifier = Modifier.padding(padding),
        ) {
            if (user != null) {
                FormColumn(PaddingValuesZero) {
                    state.actionError?.let { ErrorBanner(it) }
                    FormCard(stringResource(R.string.people_details)) {
                        Detail(stringResource(R.string.account_role), roleLabel(user))
                        Detail(
                            stringResource(R.string.people_state),
                            stringResource(
                                if (user.state ==
                                    "active"
                                ) {
                                    R.string.people_active
                                } else {
                                    R.string.myaccount_off
                                },
                            ),
                        )
                        Detail(stringResource(R.string.people_may), permissionsSummary(user))
                        if (state.isKid(user)) Detail(stringResource(R.string.people_rating), user.maxRating)
                        Detail(
                            stringResource(R.string.myaccount_tf),
                            stringResource(if (user.twoFactor) R.string.myaccount_on else R.string.myaccount_off),
                        )
                        user.links.forEach { Detail(it.provider.replaceFirstChar { c -> c.uppercase() }, it.name) }
                    }
                    if (state.mayChange(user)) {
                        FormCard(stringResource(R.string.people_actions)) {
                            ActionRow(stringResource(R.string.people_change)) { dialog = Dialog.CHANGE }
                            ActionRow(stringResource(R.string.people_set_password)) { dialog = Dialog.PASSWORD }
                            if (user.twoFactor) ActionRow(stringResource(R.string.people_reset_tf)) { dialog = Dialog.RESET }
                            if (user.state == "active") {
                                ActionRow(stringResource(R.string.people_turn_off)) { dialog = Dialog.TURN_OFF }
                            } else {
                                ActionRow(stringResource(R.string.people_turn_on)) { vm.turnOn(user) }
                            }
                            if (state.viewerOwner) {
                                ActionRow(stringResource(R.string.people_server_account)) {
                                    vm.loadServers()
                                    dialog = Dialog.SERVER
                                }
                            }
                            ActionRow(stringResource(R.string.people_delete), destructive = true) { dialog = Dialog.DELETE }
                        }
                    }
                    if (state.maySeeDevices(user)) {
                        FormCard(stringResource(R.string.myaccount_devices)) {
                            ActionRow(
                                stringResource(R.string.people_devices_of, user.name),
                            ) { onOpen(PeopleDevicesRoute(user.id, user.name)) }
                        }
                    }
                    if (state.viewerOwner && user.ssoLinks.isNotEmpty()) {
                        FormCard(stringResource(R.string.people_sso)) {
                            user.ssoLinks.forEach { link ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        stringResource(R.string.myaccount_sso_as, link.issuer, link.name),
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    TextButton(onClick = { vm.unlinkSso(user, link) }) { Text(stringResource(R.string.myaccount_unlink)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (user != null) {
        when (dialog) {
            Dialog.CHANGE -> ChangeDialog(user, vm, state.viewerOwner, state.grantable) { dialog = Dialog.NONE }
            Dialog.PASSWORD ->
                PasswordDialog(user, vm, state.problem == PeopleProblem.PASSWORD_SHORT, state.minPasswordLength) {
                    dialog =
                        Dialog.NONE
                }
            Dialog.TURN_OFF ->
                Confirm(
                    stringResource(
                        R.string.people_turn_off_title,
                        user.name,
                    ),
                    stringResource(R.string.people_turn_off_body),
                    R.string.people_turn_off,
                    {
                        vm.turnOff(user)
                    },
                ) { dialog = Dialog.NONE }
            Dialog.RESET ->
                Confirm(
                    stringResource(
                        R.string.people_reset_tf_title,
                        user.name,
                    ),
                    stringResource(R.string.people_reset_tf_body),
                    R.string.people_reset,
                    {
                        vm.resetTwoFactor(user)
                    },
                ) { dialog = Dialog.NONE }
            Dialog.DELETE ->
                Confirm(
                    stringResource(
                        R.string.people_delete_title,
                        user.name,
                    ),
                    stringResource(R.string.people_delete_body),
                    R.string.people_delete_person,
                    {
                        vm.delete(user)
                    },
                ) { dialog = Dialog.NONE }
            Dialog.SERVER -> ServerDialog(user, vm) { dialog = Dialog.NONE }
            Dialog.NONE -> Unit
        }
        state.noWayIn?.let {
            Confirm(
                stringResource(R.string.people_no_way_in_title),
                stringResource(R.string.people_no_way_in_body, user.name),
                R.string.people_unlink_anyway,
                { vm.confirmNoWayIn() },
            ) { vm.dismissNoWayIn() }
        }
    }
    ReauthSheet(vm.presenter.reauth)
}

@Composable
private fun Detail(
    label: String,
    value: String,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.4f),
        )
    }
}

@Composable
private fun ActionRow(
    label: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    Text(
        label,
        style = MaterialTheme.typography.bodyLarge,
        color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = Spacing.Small),
    )
    HorizontalDivider()
}

@Composable
private fun Confirm(
    title: String,
    body: String,
    action: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) { Text(stringResource(action), color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
private fun PasswordDialog(
    user: TitleUser,
    vm: PeopleViewModel,
    tooShort: Boolean,
    minimum: Int,
    onDismiss: () -> Unit,
) {
    var password by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.people_password_title, user.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                Text(stringResource(R.string.people_password_note, user.name), style = MaterialTheme.typography.bodyMedium)
                PasswordField(
                    password,
                    { password = it },
                    stringResource(R.string.myaccount_pw_new),
                    error = if (tooShort) stringResource(R.string.myaccount_pw_short, minimum) else null,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = password.isNotEmpty(), onClick = {
                vm.setPassword(user, password)
                onDismiss()
            }) { Text(stringResource(R.string.people_set_password)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
private fun ChangeDialog(
    user: TitleUser,
    vm: PeopleViewModel,
    owner: Boolean,
    grantable: List<String>,
    onDismiss: () -> Unit,
) {
    var admin by rememberSaveable { mutableStateOf(user.role == "admin") }
    var perms by rememberSaveable { mutableStateOf(user.perms) }
    var rating by rememberSaveable { mutableStateOf(user.maxRating) }
    val preset = peoplePreset(perms, rating)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.people_change_title, user.name)) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                if (owner) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.role_admin), modifier = Modifier.weight(1f))
                        Switch(checked = admin, onCheckedChange = {
                            admin = it
                            if (it) rating = ""
                        })
                    }
                }
                if (!admin) {
                    Text(stringResource(R.string.people_preset), style = MaterialTheme.typography.titleSmall)
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        listOf(PeoplePreset.MANAGER, PeoplePreset.MEMBER, PeoplePreset.KID, PeoplePreset.CUSTOM).forEach { option ->
                            FilterChip(selected = preset == option, onClick = {
                                if (option != PeoplePreset.CUSTOM) {
                                    perms = option.perms.filter { it in grantable }
                                    rating = option.rating
                                }
                            }, label = { Text(presetLabel(option)) })
                        }
                    }
                    if (rating.isNotEmpty()) RatingPicker(rating) { rating = it }
                }
                grantable.forEach { perm ->
                    Row(
                        Modifier.fillMaxWidth().clickable { perms = if (perm in perms) perms - perm else perms + perm },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = perm in perms, onCheckedChange = null)
                        Text(permLabel(perm), modifier = Modifier.padding(start = Spacing.Small))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                vm.save(user, admin, perms, rating)
                onDismiss()
            }) { Text(stringResource(R.string.myaccount_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
private fun ServerDialog(
    user: TitleUser,
    vm: PeopleViewModel,
    onDismiss: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.people_server_account)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                user.links.filter { it.provider != "plex" }.forEach { link ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.myaccount_media_as, link.provider.replaceFirstChar { it.uppercase() }, link.name),
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { vm.unlinkMedia(user, link.provider) }) { Text(stringResource(R.string.myaccount_unlink)) }
                    }
                }
                state.servers.forEach { server ->
                    Text(server.name, style = MaterialTheme.typography.titleSmall)
                    if (server.error.isNotEmpty()) Text(server.error, color = MaterialTheme.colorScheme.error)
                    server.accounts.forEach { account ->
                        val taken = account.takenBy != 0L && account.takenBy != user.id
                        Text(
                            account.name + if (taken) " · " + stringResource(R.string.people_linked_to, account.takenName) else "",
                            color = if (taken) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !taken) {
                                        vm.linkMedia(user, server.id, account.id)
                                        onDismiss()
                                    }.padding(vertical = Spacing.Small),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.myaccount_close)) } },
    )
}
