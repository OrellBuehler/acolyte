package ch.orellbuehler.acolyte.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.discover.CardKind
import ch.orellbuehler.acolyte.feature.discover.HiddenRow
import ch.orellbuehler.acolyte.feature.discover.HideDone
import ch.orellbuehler.acolyte.feature.discover.HideFor
import ch.orellbuehler.acolyte.feature.discover.HideWhat
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.theme.Spacing
import kotlinx.coroutines.launch

class HideSheetViewModel(
    sdk: AcolyteSdk,
    kind: CardKind,
    tmdb: String,
    name: String,
    genres: List<String>,
) : ViewModel() {
    private val presenter = sdk.hideTitle(kind, tmdb, name, genres)

    val state = presenter.state

    fun load() {
        viewModelScope.launch { presenter.load() }
    }

    fun pickGenre(genre: String) = presenter.pickGenre(genre)

    fun pickEveryone(everyone: Boolean) = presenter.pickEveryone(everyone)

    fun pickPerson(id: Long) = presenter.pickPerson(id)

    fun hide(done: (HideDone) -> Unit) {
        viewModelScope.launch { presenter.hide()?.let(done) }
    }

    fun showAgain(id: Long) {
        viewModelScope.launch { presenter.showAgain(id) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HideFromBrowsingSheet(
    kind: CardKind,
    tmdb: String,
    name: String,
    genres: List<String>,
    onDismiss: () -> Unit,
    onHidden: (HideDone) -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel(key = "hide-${kind.wire}-$tmdb") { HideSheetViewModel(sdk, kind, tmdb, name, genres) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.load() }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.Screen)
                .padding(bottom = Spacing.XLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Text(stringResource(R.string.discover_hide_title, name), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.discover_hide_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.hidden.isNotEmpty()) {
                Text(stringResource(R.string.discover_hide_now), style = MaterialTheme.typography.titleSmall)
                state.hidden.forEach { row ->
                    HiddenLine(row, onShowAgain = { viewModel.showAgain(row.id) })
                }
            }
            if (state.genres.isNotEmpty()) {
                GenrePicker(kind, name, state.genres, state.genre, viewModel::pickGenre)
            }
            if (state.people.isNotEmpty()) {
                Text(stringResource(R.string.discover_hide_for), style = MaterialTheme.typography.titleSmall)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = state.everyone,
                        onClick = { viewModel.pickEveryone(true) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                    ) { Text(stringResource(R.string.discover_hide_everyone)) }
                    SegmentedButton(
                        selected = !state.everyone,
                        onClick = { viewModel.pickEveryone(false) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                    ) { Text(stringResource(R.string.discover_hide_one_person)) }
                }
                if (!state.everyone) {
                    PersonPicker(state.people.map { it.id to it.name }, state.personId, state.needsPerson, viewModel::pickPerson)
                }
            }
            state.error?.let { ErrorBanner(it, Modifier.padding(horizontal = 0.dp)) }
            state.status.error?.let { Text(errorText(it), color = MaterialTheme.colorScheme.error) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Small, Alignment.End)) {
                OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
                Button(onClick = { viewModel.hide(onHidden) }, enabled = !state.saving) {
                    if (state.saving) CircularProgressIndicator(Modifier.padding(end = Spacing.Small).padding(2.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.discover_hide_button))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenrePicker(
    kind: CardKind,
    name: String,
    genres: List<String>,
    selected: String,
    onPick: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val only = stringResource(R.string.discover_hide_only, name)
    val label: @Composable (String) -> String = { genre ->
        if (genre.isEmpty()) {
            only
        } else if (kind == CardKind.SHOW) {
            stringResource(R.string.discover_hide_all_shows, genreText(genre))
        } else {
            stringResource(R.string.discover_hide_all_movies, genreText(genre))
        }
    }
    val current = label(selected)
    val options = listOf("") + genres
    val labels = options.map { label(it) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = current,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.discover_hide_what)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEachIndexed { index, genre ->
                DropdownMenuItem(
                    text = { Text(labels[index]) },
                    onClick = {
                        onPick(genre)
                        open = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonPicker(
    people: List<Pair<Long, String>>,
    selected: Long,
    needsPerson: Boolean,
    onPick: (Long) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = people.firstOrNull { it.first == selected }?.second.orEmpty(),
            onValueChange = {},
            readOnly = true,
            isError = needsPerson,
            placeholder = { Text(stringResource(R.string.discover_hide_pick_person)) },
            supportingText = if (needsPerson) ({ Text(stringResource(R.string.discover_hide_pick_person)) }) else null,
            label = { Text(stringResource(R.string.discover_hide_person)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            people.forEach { (id, person) ->
                DropdownMenuItem(
                    text = { Text(person) },
                    onClick = {
                        onPick(id)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
fun HiddenLine(
    row: HiddenRow,
    onShowAgain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(if (row.genre) genreText(row.name) else row.name, style = MaterialTheme.typography.titleSmall)
            Text(hiddenLine(row), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onShowAgain) { Text(stringResource(R.string.discover_hide_show_again)) }
    }
}

@Composable
fun hiddenLine(row: HiddenRow): String {
    val what =
        stringResource(
            when (row.what) {
                HideWhat.MOVIE -> R.string.discover_kind_movie
                HideWhat.SHOW -> R.string.discover_kind_show
                HideWhat.MOVIE_GENRE -> R.string.discover_hide_movie_genre
                HideWhat.SHOW_GENRE -> R.string.discover_hide_show_genre
            },
        )
    val who =
        when (row.who) {
            HideFor.EVERYONE -> stringResource(R.string.discover_hide_everyone)
            HideFor.PERSON -> row.person
            HideFor.REMOVED_PERSON -> stringResource(R.string.discover_hide_removed_person)
        }
    return "$what · $who"
}
