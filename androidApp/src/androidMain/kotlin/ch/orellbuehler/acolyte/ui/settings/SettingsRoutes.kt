package ch.orellbuehler.acolyte.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.ui.components.EmptyView
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.nav.SettingsAiRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsAlertsRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsApiRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsAppearanceRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsBackupsRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsDangerRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsDownloadersRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsHealthRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsLogsRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsMediaServersRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsMetadataRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsMoveRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsNamingRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsPartsRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsPickRulesRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsRemoteAccessRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsSchedulesRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsSearchRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsSignInRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsSourcesRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsSubtitlesRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsThisAppRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsTitleDefaultsRoute

fun NavGraphBuilder.settingsRoutes(nav: NavHostController) {
    val pop: () -> Unit = { nav.popBackStack() }
    val open: (Any) -> Unit = { nav.navigate(it) }
    composable<SettingsSignInRoute> { SettingsSignInScreen(pop, open) }
    composable<SettingsApiRoute> { SettingsApiScreen(pop, open) }
    composable<SettingsAiRoute> { SettingsAiScreen(pop, open) }
    composable<SettingsRemoteAccessRoute> { SettingsRemoteAccessScreen(pop, open) }
    composable<SettingsAppearanceRoute> { SettingsAppearanceScreen(pop, open) }
    composable<SettingsTitleDefaultsRoute> { SettingsTitleDefaultsScreen(pop, open) }
    composable<SettingsMetadataRoute> { SettingsMetadataScreen(pop, open) }
    composable<SettingsAlertsRoute> { SettingsAlertsScreen(pop, open) }
    composable<SettingsBackupsRoute> { SettingsBackupsScreen(pop, open) }
    composable<SettingsLogsRoute> { SettingsLogsScreen(pop, open) }
    composable<SettingsHealthRoute> { SettingsHealthScreen(pop, open) }
    composable<SettingsPartsRoute> { SettingsPartsScreen(pop, open) }
    composable<SettingsThisAppRoute> { SettingsThisAppScreen(pop, open) }
    composable<SettingsDangerRoute> { SettingsDangerScreen(pop, open) }
    composable<SettingsSchedulesRoute> { SettingsSchedulesScreen(pop, open) }
    composable<SettingsMediaServersRoute> { SettingsMediaServersScreen(pop, open) }
    settingsPlaceholderRoutes(pop)
}

private fun NavGraphBuilder.settingsPlaceholderRoutes(pop: () -> Unit) {
    composable<SettingsSourcesRoute> { SettingsPagePlaceholder(R.string.settings_page_sources, pop) }
    composable<SettingsDownloadersRoute> { SettingsPagePlaceholder(R.string.settings_page_downloaders, pop) }
    composable<SettingsPickRulesRoute> { SettingsPagePlaceholder(R.string.settings_page_pick_rules, pop) }
    composable<SettingsNamingRoute> { SettingsPagePlaceholder(R.string.settings_page_naming, pop) }
    composable<SettingsSubtitlesRoute> { SettingsPagePlaceholder(R.string.settings_page_subtitles, pop) }
    composable<SettingsSearchRoute> { SettingsPagePlaceholder(R.string.settings_page_search, pop) }
    composable<SettingsMoveRoute> { SettingsPagePlaceholder(R.string.settings_page_move, pop) }
}

@Composable
fun SettingsPagePlaceholder(
    title: Int,
    onBack: () -> Unit,
) {
    ScreenScaffold(title = stringResource(title), onBack = onBack) { padding ->
        EmptyView(
            title = stringResource(R.string.placeholder_title),
            message = stringResource(R.string.placeholder_message),
            modifier = Modifier.padding(padding),
        )
    }
}
