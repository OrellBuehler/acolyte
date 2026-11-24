package ch.orellbuehler.acolyte.ui.music

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.nav.TitleRoute
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.title.RemoveContent
import ch.orellbuehler.acolyte.ui.title.RenameContent
import ch.orellbuehler.acolyte.ui.title.TitleSheet
import ch.orellbuehler.acolyte.ui.title.TitleViewModel

enum class MusicAlbumTool {
    RENAME,
    DELETE,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicAlbumToolSheet(
    tool: MusicAlbumTool,
    titleId: Long,
    albumId: Long,
    name: String,
    onClose: (removed: Boolean) -> Unit,
) {
    val sdk = LocalSdk.current
    val tools = viewModel(key = "music-tool-$titleId-$albumId-${tool.name}") { TitleViewModel(sdk, TitleRoute("artist", "", titleId)) }
    val sheet =
        remember(tool) {
            when (tool) {
                MusicAlbumTool.RENAME -> TitleSheet.Rename(sdk.musicRename(titleId))
                MusicAlbumTool.DELETE -> TitleSheet.Remove(sdk.musicRemove(titleId, name, albumId))
            }
        }
    var opened by remember { mutableStateOf(false) }
    var removed by remember { mutableStateOf(false) }
    LaunchedEffect(tools) {
        tools.open(sheet)
        opened = true
    }
    if (sheet is TitleSheet.Remove) {
        val remove by sheet.presenter.state.collectAsStateWithLifecycle()
        LaunchedEffect(remove.done) { if (remove.done) removed = true }
    }
    val open = tools.sheet
    LaunchedEffect(opened, open) { if (opened && open == null) onClose(removed) }
    if (open == null) return
    ModalBottomSheet(onDismissRequest = { tools.close() }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.XLarge)
                .navigationBarsPadding(),
        ) {
            when (open) {
                is TitleSheet.Rename -> RenameContent(open.presenter, name, tools)
                is TitleSheet.Remove -> RemoveContent(open.presenter, tools)
                else -> Unit
            }
        }
    }
}
