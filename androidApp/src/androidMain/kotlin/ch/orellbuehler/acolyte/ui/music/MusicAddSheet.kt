package ch.orellbuehler.acolyte.ui.music

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.music.MusicAddOutcome
import ch.orellbuehler.acolyte.feature.music.MusicAddPresenter
import ch.orellbuehler.acolyte.feature.music.MusicAddState
import ch.orellbuehler.acolyte.feature.music.MusicOptions
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.LoadingView
import ch.orellbuehler.acolyte.ui.components.NoticeBanner
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.library.PresenterViewModel
import ch.orellbuehler.acolyte.ui.requests.DoneView
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.PickerField
import ch.orellbuehler.acolyte.ui.requests.RouteSheet
import ch.orellbuehler.acolyte.ui.requests.SheetFooter
import ch.orellbuehler.acolyte.ui.requests.SwitchRow
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun MusicAddSheet(
    mbid: String,
    name: String,
    onClose: () -> Unit,
    onOpenFolders: (() -> Unit)? = null,
) {
    val sdk = LocalSdk.current
    val vm = viewModel(key = "music-add-$mbid") { PresenterViewModel(sdk.musicAdd(mbid, name)) { load() } }
    val state by vm.presenter.state.collectAsStateWithLifecycle()
    RouteSheet(onClose) {
        when {
            state.status.phase != Phase.READY -> LoadingView(Modifier.padding(Spacing.XLarge))
            state.outcome == MusicAddOutcome.ADDED ->
                DoneView(stringResource(R.string.music_add_added, state.name), stringResource(R.string.music_add_filling), onClose)
            state.outcome == MusicAddOutcome.ALREADY ->
                DoneView(stringResource(R.string.music_note_already_in_library, state.name), "", onClose)
            else -> AddForm(state, vm, onClose, onOpenFolders)
        }
    }
}

@Composable
private fun AddForm(
    state: MusicAddState,
    vm: PresenterViewModel<MusicAddPresenter>,
    onClose: () -> Unit,
    onOpenFolders: (() -> Unit)?,
) {
    Text(stringResource(R.string.music_add_title, state.name), style = MaterialTheme.typography.titleLarge)
    if (state.disambiguation.isNotEmpty()) Hint(state.disambiguation)
    if (state.titleId > 0) NoticeBanner(stringResource(R.string.music_note_already_in_library, state.name))
    if (state.neverAdd) NoticeBanner(stringResource(R.string.music_add_never))
    if (state.noFolder) {
        NoticeBanner(stringResource(R.string.music_add_no_folder))
        if (onOpenFolders != null) OutlinedButton(onClick = onOpenFolders) { Text(stringResource(R.string.music_add_open_folders)) }
    } else if (state.folders.isEmpty()) {
        Hint(stringResource(R.string.music_add_hands_off))
    } else {
        PickerField(
            label = stringResource(R.string.music_add_folder),
            options = state.folders.map { it.id to "${it.label.ifEmpty { it.path }} (${it.path})" },
            selected = state.folderId,
            onSelect = { id -> vm.presenter.pickFolder(id) },
        )
    }
    if (state.rules.isNotEmpty()) {
        PickerField(
            label = stringResource(R.string.music_add_rules),
            options = state.rules.map { it.id to it.name },
            selected = state.rulesId,
            onSelect = { id -> vm.presenter.pickRules(id) },
            hint = stringResource(R.string.music_add_rules_hint),
        )
    }
    MusicOptionSelect(
        label = stringResource(R.string.music_add_choice),
        group = MusicOptionGroup.CHOICE,
        options = MusicOptions.albumChoices,
        selected = state.albumChoice,
        onSelect = { id -> vm.presenter.setAlbumChoice(id) },
    )
    MusicOptionSelect(
        label = stringResource(R.string.music_add_new),
        group = MusicOptionGroup.NEW,
        options = MusicOptions.newAlbums,
        selected = state.newAlbums,
        onSelect = { id -> vm.presenter.setNewAlbums(id) },
        hint = stringResource(R.string.music_add_new_hint),
    )
    MusicOptionChecks(
        title = stringResource(R.string.music_add_kinds),
        group = MusicOptionGroup.TYPE,
        options = MusicOptions.albumTypes,
        selected = state.albumTypes,
        onToggle = { id, on -> vm.presenter.toggleAlbumType(id, on) },
        hint = stringResource(R.string.music_add_kinds_hint),
    )
    MusicOptionChecks(
        title = stringResource(R.string.music_add_extra),
        group = MusicOptionGroup.EXTRA,
        options = MusicOptions.secondaryTypes,
        selected = state.secondaryTypes,
        onToggle = { id, on -> vm.presenter.toggleSecondary(id, on) },
        hint = stringResource(R.string.music_add_extra_hint),
    )
    MusicOptionChecks(
        title = stringResource(R.string.music_add_statuses),
        group = MusicOptionGroup.STATUS,
        options = MusicOptions.statuses,
        selected = state.statuses,
        onToggle = { id, on -> vm.presenter.toggleStatus(id, on) },
    )
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        SwitchRow(
            stringResource(R.string.music_add_keep),
            state.keep,
            { on -> vm.presenter.setKeep(on) },
            hint = stringResource(R.string.music_add_keep_hint),
        )
        if (state.keep) Hint(stringResource(R.string.music_add_keep_note))
    }
    SwitchRow(
        stringResource(R.string.music_add_follow),
        state.follow,
        { on -> vm.presenter.setFollow(on) },
        hint = stringResource(R.string.music_add_follow_hint),
    )
    state.error?.let { FailText(errorText(it)) }
    SheetFooter(
        primary = stringResource(R.string.music_add_submit),
        enabled = state.canSubmit,
        busy = state.busy,
        onCancel = onClose,
        onPrimary = { vm.run { submit() } },
    )
}
