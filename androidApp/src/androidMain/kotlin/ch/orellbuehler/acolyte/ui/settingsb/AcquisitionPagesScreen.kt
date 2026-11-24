package ch.orellbuehler.acolyte.ui.settingsb

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.acquisition.AcquisitionPage
import ch.orellbuehler.acolyte.feature.acquisition.acquisitionPages
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.nav.Routes

@Composable
fun AcquisitionPagesScreen(
    account: Account,
    onBack: (() -> Unit)?,
    onOpen: (Any) -> Unit,
) {
    ScreenScaffold(title = stringResource(R.string.section_settings), onBack = onBack) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(account.acquisitionPages()) { page ->
                ListItem(
                    headlineContent = { Text(stringResource(acquisitionTitle(page))) },
                    supportingContent = { Text(stringResource(acquisitionHint(page))) },
                    trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable { onOpen(acquisitionRoute(page)) },
                )
                HorizontalDivider()
            }
        }
    }
}

fun acquisitionTitle(page: AcquisitionPage): Int =
    when (page) {
        AcquisitionPage.SOURCES -> R.string.acq_page_sources
        AcquisitionPage.DOWNLOADERS -> R.string.acq_page_downloaders
        AcquisitionPage.PICK_RULES -> R.string.acq_page_quality
        AcquisitionPage.NAMING -> R.string.acq_page_naming
        AcquisitionPage.SUBTITLES -> R.string.acq_page_subtitles
        AcquisitionPage.SEARCH -> R.string.acq_page_search
        AcquisitionPage.MOVE -> R.string.acq_page_move
    }

fun acquisitionHint(page: AcquisitionPage): Int =
    when (page) {
        AcquisitionPage.SOURCES -> R.string.acq_hint_sources
        AcquisitionPage.DOWNLOADERS -> R.string.acq_hint_downloaders
        AcquisitionPage.PICK_RULES -> R.string.acq_hint_quality
        AcquisitionPage.NAMING -> R.string.acq_hint_naming
        AcquisitionPage.SUBTITLES -> R.string.acq_hint_subtitles
        AcquisitionPage.SEARCH -> R.string.acq_hint_search
        AcquisitionPage.MOVE -> R.string.acq_hint_move
    }

fun acquisitionRoute(page: AcquisitionPage): Any =
    when (page) {
        AcquisitionPage.SOURCES -> Routes.settingsSources
        AcquisitionPage.DOWNLOADERS -> Routes.settingsDownloaders
        AcquisitionPage.PICK_RULES -> Routes.settingsPickRules
        AcquisitionPage.NAMING -> Routes.settingsNaming
        AcquisitionPage.SUBTITLES -> Routes.settingsSubtitles
        AcquisitionPage.SEARCH -> Routes.settingsSearch
        AcquisitionPage.MOVE -> Routes.settingsMove
    }
