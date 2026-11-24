package ch.orellbuehler.acolyte.ui.people

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.model.PeopleInvite
import ch.orellbuehler.acolyte.api.model.TitleUser
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.account.PEOPLE_RATINGS
import ch.orellbuehler.acolyte.feature.account.PeopleInviteDraft
import ch.orellbuehler.acolyte.feature.account.PeopleInviteState
import ch.orellbuehler.acolyte.feature.account.PeopleNotice
import ch.orellbuehler.acolyte.feature.account.PeoplePreset
import ch.orellbuehler.acolyte.feature.account.PeopleProblem
import ch.orellbuehler.acolyte.feature.account.PeopleState
import ch.orellbuehler.acolyte.feature.account.isOpen
import ch.orellbuehler.acolyte.feature.account.peopleRatingForShows
import ch.orellbuehler.acolyte.feature.account.state
import ch.orellbuehler.acolyte.image.ImageSize
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.account.FormCard
import ch.orellbuehler.acolyte.ui.account.ToastOnce
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.ReauthSheet
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.ServerImage
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.components.relativeTime
import ch.orellbuehler.acolyte.ui.nav.PeopleDetailRoute
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun noticeText(
    notice: PeopleNotice?,
    name: String,
): String? =
    when (notice) {
        PeopleNotice.INVITE_REVOKED -> stringResource(R.string.people_notice_revoked)
        PeopleNotice.SAVED -> stringResource(R.string.people_notice_saved, name)
        PeopleNotice.TURNED_OFF -> stringResource(R.string.people_notice_off, name)
        PeopleNotice.TURNED_ON -> stringResource(R.string.people_notice_on, name)
        PeopleNotice.PASSWORD_SET -> stringResource(R.string.people_notice_password, name)
        PeopleNotice.TWO_FACTOR_RESET -> stringResource(R.string.people_notice_two_factor, name)
        PeopleNotice.DELETED -> stringResource(R.string.people_notice_deleted, name)
        PeopleNotice.SSO_UNLINKED -> stringResource(R.string.people_notice_sso, name)
        PeopleNotice.MEDIA_LINKED -> stringResource(R.string.people_notice_media_linked, name)
        PeopleNotice.MEDIA_UNLINKED -> stringResource(R.string.people_notice_media_unlinked, name)
        null -> null
    }

@Composable
fun PeopleScreen(
    account: Account,
    onOpen: (Any) -> Unit,
    onBack: (() -> Unit)?,
) {
    val sdk = LocalSdk.current
    val vm = viewModel { PeopleViewModel(sdk) }
    val state by vm.state.collectAsStateWithLifecycle()
    var inviting by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        if (vm.state.value.status.phase == ch.orellbuehler.acolyte.feature.Phase.READY) vm.refresh()
        onPauseOrDispose {}
    }
    ToastOnce(noticeText(state.notice, state.noticeName)) { vm.consume() }
    ScreenScaffold(
        title = stringResource(R.string.section_people),
        onBack = onBack,
        actions = {
            if (state.status.phase == ch.orellbuehler.acolyte.feature.Phase.READY) {
                IconButton(onClick = { inviting = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.people_invite))
                }
            }
        },
    ) { padding ->
        StatusContent(state.status, isEmpty = false, onRefresh = { vm.refresh() }, modifier = Modifier.padding(padding)) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(Spacing.Screen),
                verticalArrangement = Arrangement.spacedBy(Spacing.Large),
            ) {
                state.actionError?.let { ErrorBanner(it) }
                state.fresh?.let { fresh ->
                    FreshInvite(fresh.url, fresh.singleUse) { vm.dismissInvite() }
                }
                FormCard(stringResource(R.string.people_title)) {
                    state.users.forEach { user ->
                        PersonRow(user, state) { onOpen(PeopleDetailRoute(user.id)) }
                    }
                }
                FormCard(stringResource(R.string.people_invites)) {
                    if (state.invites.isEmpty()) {
                        Text(
                            stringResource(R.string.people_invites_none),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    state.invites.forEach { invite -> InviteRow(invite) { vm.revoke(invite) } }
                }
            }
        }
    }
    if (inviting) {
        InviteSheet(state, onDismiss = { inviting = false }) { draft ->
            vm.makeInvite(draft)
            inviting = false
        }
    }
    ReauthSheet(vm.presenter.reauth)
}

@Composable
private fun FreshInvite(
    url: String,
    single: Boolean,
    onClose: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    FormCard(stringResource(R.string.people_fresh_title)) {
        Text(
            stringResource(if (single) R.string.people_fresh_single else R.string.people_fresh_many),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(url, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            OutlinedButton(onClick = { clipboard.setText(AnnotatedString(url)) }) { Text(stringResource(R.string.myaccount_copy)) }
            OutlinedButton(onClick = {
                val send =
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, url)
                    }
                context.startActivity(Intent.createChooser(send, null))
            }) { Text(stringResource(R.string.myaccount_share)) }
            TextButton(onClick = onClose) { Text(stringResource(R.string.myaccount_close)) }
        }
    }
}

@Composable
private fun PersonRow(
    user: TitleUser,
    state: PeopleState,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        ServerImage(
            source = user.avatar,
            contentDescription = null,
            size = ImageSize.THUMB,
            placeholderName = user.name,
            modifier = Modifier.size(40.dp).clip(CircleShape),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(user.name, style = MaterialTheme.typography.bodyLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.XSmall),
                verticalArrangement = Arrangement.spacedBy(Spacing.XSmall),
            ) {
                StatusChip(roleLabel(user), if (user.role == "member") Tone.IDLE else Tone.INFO)
                if (user.state != "active") StatusChip(stringResource(R.string.myaccount_off), Tone.FAIL)
                if (user.twoFactor) StatusChip(stringResource(R.string.people_two_factor), Tone.OK)
                if (state.isKid(user)) StatusChip(stringResource(R.string.people_kid_up_to, user.maxRating), Tone.WAIT)
            }
            Text(
                listOfNotNull(
                    user.links
                        .joinToString(" · ") {
                            "${it.provider.replaceFirstChar { c ->
                                c.uppercase()
                            }}: ${it.name}"
                        }.ifEmpty { null },
                    permissionsSummary(user),
                    if (user.lastSeenAt.isEmpty()) {
                        stringResource(R.string.people_not_seen)
                    } else {
                        stringResource(R.string.people_seen, relativeTime(user.lastSeenAt) ?: "")
                    },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (user.offReason.isNotEmpty()) {
                Text(
                    stringResource(R.string.people_removed_on_server, user.offReason.replaceFirstChar { it.uppercase() }),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun InviteRow(
    invite: PeopleInvite,
    onRevoke: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                invite.note.ifEmpty {
                    stringResource(
                        R.string.people_invite_of_role,
                        stringResource(if (invite.role == "admin") R.string.role_admin else R.string.role_member),
                    )
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            val status =
                when (invite.state) {
                    PeopleInviteState.USED -> stringResource(R.string.people_invite_used, invite.usedBy.ifEmpty { "?" })
                    PeopleInviteState.ENDED -> stringResource(R.string.people_invite_ended)
                    PeopleInviteState.OPEN ->
                        if (invite.neverExpires) {
                            stringResource(
                                R.string.people_invite_open_forever,
                            )
                        } else {
                            stringResource(R.string.people_invite_open)
                        }
                }
            Text(
                listOfNotNull(
                    stringResource(R.string.people_invite_by, invite.createdBy),
                    status,
                    if (invite.maxRating.isNotEmpty()) stringResource(R.string.people_kid_up_to, invite.maxRating) else null,
                    if (invite.joined.isNotEmpty()) stringResource(R.string.people_invite_joined, invite.joined.size) else null,
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (invite.isOpen) TextButton(onClick = onRevoke) { Text(stringResource(R.string.people_revoke)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InviteSheet(
    state: PeopleState,
    onDismiss: () -> Unit,
    onMake: (PeopleInviteDraft) -> Unit,
) {
    var preset by rememberSaveable { mutableStateOf(PeoplePreset.MEMBER) }
    var perms by rememberSaveable { mutableStateOf(PeoplePreset.MEMBER.perms) }
    var rating by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var days by rememberSaveable { mutableStateOf("7") }
    var uses by rememberSaveable { mutableStateOf("1") }
    var more by rememberSaveable { mutableStateOf(false) }
    val presets = PeoplePreset.entries.filter { it != PeoplePreset.ADMIN || state.canMakeAdmin }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = Spacing.Screen).padding(bottom = Spacing.XLarge).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Text(stringResource(R.string.people_invite), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.people_preset), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                presets.forEach { option ->
                    FilterChip(
                        selected = preset == option,
                        onClick = {
                            preset = option
                            if (option != PeoplePreset.CUSTOM) {
                                perms = option.perms.filter { it in state.grantable }
                                rating = option.rating
                            }
                        },
                        label = { Text(presetLabel(option)) },
                    )
                }
            }
            if (preset == PeoplePreset.KID) {
                RatingPicker(rating) {
                    rating = it
                }
            }
            Text(stringResource(R.string.people_may), style = MaterialTheme.typography.titleSmall)
            state.grantable.forEach { perm ->
                Row(
                    Modifier.fillMaxWidth().clickable { perms = if (perm in perms) perms - perm else perms + perm },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = perm in perms, onCheckedChange = null)
                    Text(permLabel(perm), modifier = Modifier.padding(start = Spacing.Small))
                }
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(100) },
                label = { Text(stringResource(R.string.people_note)) },
                placeholder = { Text(stringResource(R.string.people_note_example)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = days,
                onValueChange = { days = it.filter(Char::isDigit).take(2) },
                label = { Text(stringResource(R.string.people_days)) },
                supportingText = { if (state.viewerOwner) Text(stringResource(R.string.people_days_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = { more = !more }) { Text(stringResource(R.string.people_more_options)) }
            if (more) {
                OutlinedTextField(
                    value = uses,
                    onValueChange = { uses = it.filter(Char::isDigit).take(4) },
                    label = { Text(stringResource(R.string.people_uses)) },
                    supportingText = { if (state.viewerOwner) Text(stringResource(R.string.people_days_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            state.problem?.let {
                Text(
                    stringResource(if (it == PeopleProblem.INVALID_DAYS) R.string.people_problem_days else R.string.people_problem_uses),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.busy,
                onClick = {
                    onMake(
                        PeopleInviteDraft(
                            admin = preset == PeoplePreset.ADMIN,
                            perms = perms,
                            rating = if (preset == PeoplePreset.KID) rating.ifEmpty { "PG" } else "",
                            note = note,
                            days = days.toIntOrNull() ?: 7,
                            maxUses = uses.toIntOrNull() ?: 1,
                        ),
                    )
                },
            ) { Text(stringResource(R.string.people_make_invite)) }
        }
    }
}

@Composable
fun RatingPicker(
    rating: String,
    onPick: (String) -> Unit,
) {
    val current = rating.ifEmpty { "PG" }
    Text(stringResource(R.string.people_rating), style = MaterialTheme.typography.titleSmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        PEOPLE_RATINGS.forEach { option ->
            FilterChip(selected = current == option, onClick = { onPick(option) }, label = { Text(option) })
        }
    }
    Text(
        stringResource(R.string.people_rating_hint, current, peopleRatingForShows(current)),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
