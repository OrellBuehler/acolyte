package ch.orellbuehler.acolyte.ui.library

import androidx.compose.runtime.Composable

@Composable
fun LibraryStatsScreen(onBack: (() -> Unit)?) = TitlePlaceholderScreen { onBack?.invoke() }

@Composable
fun LibraryDeletedScreen(
    titleId: Long,
    onBack: () -> Unit,
) = TitlePlaceholderScreen(onBack)

@Composable
fun LibraryCopiesScreen(onBack: () -> Unit) = TitlePlaceholderScreen(onBack)

@Composable
fun LibraryReleasesScreen(
    turnedDown: Boolean,
    onBack: () -> Unit,
    open: (Any) -> Unit,
) = TitlePlaceholderScreen(onBack)

@Composable
fun LibrarySubtitlesScreen(onBack: () -> Unit) = TitlePlaceholderScreen(onBack)

@Composable
fun LibraryRenameScreen(onBack: () -> Unit) = TitlePlaceholderScreen(onBack)

@Composable
fun LibraryImportScreen(
    onBack: () -> Unit,
    open: (Any) -> Unit,
) = TitlePlaceholderScreen(onBack)

@Composable
fun LibraryFilesScreen(
    path: String,
    onBack: () -> Unit,
) = TitlePlaceholderScreen(onBack)

@Composable
fun LibraryFoldersScreen(onBack: () -> Unit) = TitlePlaceholderScreen(onBack)
