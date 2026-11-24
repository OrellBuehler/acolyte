package ch.orellbuehler.acolyte.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.nav.LibraryTool
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.EmptyView
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.nav.LibraryCopiesRoute
import ch.orellbuehler.acolyte.ui.nav.LibraryDeletedRoute
import ch.orellbuehler.acolyte.ui.nav.LibraryImportFilesRoute
import ch.orellbuehler.acolyte.ui.nav.LibraryImportRoute
import ch.orellbuehler.acolyte.ui.nav.LibraryReleasesRoute
import ch.orellbuehler.acolyte.ui.nav.LibraryRenameRoute
import ch.orellbuehler.acolyte.ui.nav.LibrarySubtitlesRoute
import ch.orellbuehler.acolyte.ui.nav.Routes
import ch.orellbuehler.acolyte.ui.nav.StatsRoute

private data class ToolEntry(
    val title: Int,
    val about: Int,
    val icon: ImageVector,
    val route: Any,
)

private fun LibraryTool.entry(): ToolEntry =
    when (this) {
        LibraryTool.STATS ->
            ToolEntry(
                R.string.library_grid_tool_stats,
                R.string.library_grid_tool_stats_about,
                Icons.Rounded.Speed,
                StatsRoute,
            )
        LibraryTool.DELETED ->
            ToolEntry(
                R.string.library_grid_tool_deleted,
                R.string.library_grid_tool_deleted_about,
                Icons.Rounded.DeleteOutline,
                LibraryDeletedRoute(),
            )
        LibraryTool.COPIES ->
            ToolEntry(R.string.library_grid_tool_copies, R.string.library_grid_tool_copies_about, Icons.Rounded.Movie, LibraryCopiesRoute)
        LibraryTool.HELD ->
            ToolEntry(
                R.string.library_grid_tool_held,
                R.string.library_grid_tool_held_about,
                Icons.Rounded.HourglassTop,
                LibraryReleasesRoute(false),
            )
        LibraryTool.TURNED_DOWN ->
            ToolEntry(
                R.string.library_grid_tool_turned_down,
                R.string.library_grid_tool_turned_down_about,
                Icons.Rounded.Block,
                LibraryReleasesRoute(true),
            )
        LibraryTool.MISSING_SUBTITLES ->
            ToolEntry(
                R.string.library_grid_tool_missing_subtitles,
                R.string.library_grid_tool_missing_subtitles_about,
                Icons.Rounded.Subtitles,
                LibrarySubtitlesRoute,
            )
        LibraryTool.RENAME ->
            ToolEntry(
                R.string.library_grid_tool_rename,
                R.string.library_grid_tool_rename_about,
                Icons.Rounded.DriveFileRenameOutline,
                LibraryRenameRoute,
            )
        LibraryTool.IMPORT ->
            ToolEntry(
                R.string.library_grid_tool_import,
                R.string.library_grid_tool_import_about,
                Icons.AutoMirrored.Rounded.DriveFileMove,
                LibraryImportRoute,
            )
        LibraryTool.IMPORT_FILES ->
            ToolEntry(
                R.string.library_grid_tool_import_files,
                R.string.library_grid_tool_import_files_about,
                Icons.Rounded.UploadFile,
                LibraryImportFilesRoute(),
            )
        LibraryTool.LIBRARY_FOLDERS ->
            ToolEntry(
                R.string.library_grid_tool_folders,
                R.string.library_grid_tool_folders_about,
                Icons.Rounded.Folder,
                Routes.settingsLibraryFolders,
            )
    }

@Composable
fun LibraryToolsScreen(
    onBack: () -> Unit,
    open: (Any) -> Unit,
) {
    val sdk = LocalSdk.current
    val tools = remember { sdk.libraryTools() }
    ScreenScaffold(title = stringResource(R.string.library_tools), onBack = onBack) { padding ->
        if (tools.isEmpty()) {
            EmptyView(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(Modifier.padding(padding)) {
                items(tools, key = { it.name }) { tool ->
                    val entry = tool.entry()
                    ListItem(
                        headlineContent = { Text(stringResource(entry.title)) },
                        supportingContent = { Text(stringResource(entry.about)) },
                        leadingContent = { Icon(entry.icon, contentDescription = null) },
                        trailingContent = {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                        modifier = Modifier.clickable { open(entry.route) },
                    )
                }
            }
        }
    }
}
