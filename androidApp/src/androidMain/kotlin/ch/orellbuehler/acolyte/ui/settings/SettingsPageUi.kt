package ch.orellbuehler.acolyte.ui.settings

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.settings.SettingsGroup
import ch.orellbuehler.acolyte.feature.settings.SettingsPage
import ch.orellbuehler.acolyte.ui.nav.AccountRoute
import ch.orellbuehler.acolyte.ui.nav.OutsideListsRoute
import ch.orellbuehler.acolyte.ui.nav.PeopleRoute
import ch.orellbuehler.acolyte.ui.nav.RequestRulesRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsAiRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsAlertsRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsApiRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsAppearanceRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsBackupsRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsDangerRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsDownloadersRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsHealthRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsLibraryFoldersRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsLogsRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsMediaServersRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsMetadataRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsMoveRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsMusicRoute
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

@StringRes
fun SettingsPage.labelRes(): Int =
    when (this) {
        SettingsPage.LIBRARY_FOLDERS -> R.string.settings_page_library_folders
        SettingsPage.TITLE_DEFAULTS -> R.string.settings_page_title_defaults
        SettingsPage.MUSIC -> R.string.settings_page_music
        SettingsPage.SUBTITLES -> R.string.settings_page_subtitles
        SettingsPage.NAMING -> R.string.settings_page_naming
        SettingsPage.PARTS -> R.string.settings_page_parts
        SettingsPage.OUTSIDE_LISTS -> R.string.settings_page_outside_lists
        SettingsPage.MOVE -> R.string.settings_page_move
        SettingsPage.DOWNLOADERS -> R.string.settings_page_downloaders
        SettingsPage.SOURCES -> R.string.settings_page_sources
        SettingsPage.PICK_RULES -> R.string.settings_page_pick_rules
        SettingsPage.SEARCH -> R.string.settings_page_search
        SettingsPage.SCHEDULES -> R.string.settings_page_schedules
        SettingsPage.USERS -> R.string.settings_page_users
        SettingsPage.REQUESTS -> R.string.settings_page_requests
        SettingsPage.SIGN_IN -> R.string.settings_page_sign_in
        SettingsPage.APPEARANCE -> R.string.settings_page_appearance
        SettingsPage.MEDIA_SERVERS -> R.string.settings_page_media_servers
        SettingsPage.ALERTS -> R.string.settings_page_alerts
        SettingsPage.AI -> R.string.settings_page_ai
        SettingsPage.API -> R.string.settings_page_api
        SettingsPage.REMOTE_ACCESS -> R.string.settings_page_remote_access
        SettingsPage.METADATA -> R.string.settings_page_metadata
        SettingsPage.HEALTH -> R.string.settings_page_health
        SettingsPage.BACKUPS -> R.string.settings_page_backups
        SettingsPage.LOGS -> R.string.settings_page_logs
        SettingsPage.THIS_APP -> R.string.settings_page_this_app
        SettingsPage.DANGER -> R.string.settings_page_danger
        SettingsPage.ACCOUNT -> R.string.settings_page_account
    }

@StringRes
fun SettingsPage.aboutRes(): Int =
    when (this) {
        SettingsPage.LIBRARY_FOLDERS -> R.string.settings_page_library_folders_about
        SettingsPage.TITLE_DEFAULTS -> R.string.settings_page_title_defaults_about
        SettingsPage.MUSIC -> R.string.settings_page_music_about
        SettingsPage.SUBTITLES -> R.string.settings_page_subtitles_about
        SettingsPage.NAMING -> R.string.settings_page_naming_about
        SettingsPage.PARTS -> R.string.settings_page_parts_about
        SettingsPage.OUTSIDE_LISTS -> R.string.settings_page_outside_lists_about
        SettingsPage.MOVE -> R.string.settings_page_move_about
        SettingsPage.DOWNLOADERS -> R.string.settings_page_downloaders_about
        SettingsPage.SOURCES -> R.string.settings_page_sources_about
        SettingsPage.PICK_RULES -> R.string.settings_page_pick_rules_about
        SettingsPage.SEARCH -> R.string.settings_page_search_about
        SettingsPage.SCHEDULES -> R.string.settings_page_schedules_about
        SettingsPage.USERS -> R.string.settings_page_users_about
        SettingsPage.REQUESTS -> R.string.settings_page_requests_about
        SettingsPage.SIGN_IN -> R.string.settings_page_sign_in_about
        SettingsPage.APPEARANCE -> R.string.settings_page_appearance_about
        SettingsPage.MEDIA_SERVERS -> R.string.settings_page_media_servers_about
        SettingsPage.ALERTS -> R.string.settings_page_alerts_about
        SettingsPage.AI -> R.string.settings_page_ai_about
        SettingsPage.API -> R.string.settings_page_api_about
        SettingsPage.REMOTE_ACCESS -> R.string.settings_page_remote_access_about
        SettingsPage.METADATA -> R.string.settings_page_metadata_about
        SettingsPage.HEALTH -> R.string.settings_page_health_about
        SettingsPage.BACKUPS -> R.string.settings_page_backups_about
        SettingsPage.LOGS -> R.string.settings_page_logs_about
        SettingsPage.THIS_APP -> R.string.settings_page_this_app_about
        SettingsPage.DANGER -> R.string.settings_page_danger_about
        SettingsPage.ACCOUNT -> R.string.settings_page_account_about
    }

fun SettingsPage.route(): Any =
    when (this) {
        SettingsPage.LIBRARY_FOLDERS -> SettingsLibraryFoldersRoute
        SettingsPage.TITLE_DEFAULTS -> SettingsTitleDefaultsRoute
        SettingsPage.MUSIC -> SettingsMusicRoute
        SettingsPage.SUBTITLES -> SettingsSubtitlesRoute
        SettingsPage.NAMING -> SettingsNamingRoute
        SettingsPage.PARTS -> SettingsPartsRoute
        SettingsPage.OUTSIDE_LISTS -> OutsideListsRoute
        SettingsPage.MOVE -> SettingsMoveRoute
        SettingsPage.DOWNLOADERS -> SettingsDownloadersRoute
        SettingsPage.SOURCES -> SettingsSourcesRoute
        SettingsPage.PICK_RULES -> SettingsPickRulesRoute
        SettingsPage.SEARCH -> SettingsSearchRoute
        SettingsPage.SCHEDULES -> SettingsSchedulesRoute
        SettingsPage.USERS -> PeopleRoute
        SettingsPage.REQUESTS -> RequestRulesRoute
        SettingsPage.SIGN_IN -> SettingsSignInRoute
        SettingsPage.APPEARANCE -> SettingsAppearanceRoute
        SettingsPage.MEDIA_SERVERS -> SettingsMediaServersRoute
        SettingsPage.ALERTS -> SettingsAlertsRoute
        SettingsPage.AI -> SettingsAiRoute
        SettingsPage.API -> SettingsApiRoute
        SettingsPage.REMOTE_ACCESS -> SettingsRemoteAccessRoute
        SettingsPage.METADATA -> SettingsMetadataRoute
        SettingsPage.HEALTH -> SettingsHealthRoute
        SettingsPage.BACKUPS -> SettingsBackupsRoute
        SettingsPage.LOGS -> SettingsLogsRoute
        SettingsPage.THIS_APP -> SettingsThisAppRoute
        SettingsPage.DANGER -> SettingsDangerRoute
        SettingsPage.ACCOUNT -> AccountRoute
    }

val SettingsPage.icon: ImageVector
    get() =
        when (this) {
            SettingsPage.LIBRARY_FOLDERS -> Icons.Outlined.Folder
            SettingsPage.TITLE_DEFAULTS -> Icons.Outlined.Tv
            SettingsPage.MUSIC -> Icons.Outlined.MusicNote
            SettingsPage.SUBTITLES -> Icons.Outlined.Subtitles
            SettingsPage.NAMING -> Icons.Outlined.DriveFileRenameOutline
            SettingsPage.PARTS -> Icons.Outlined.GridView
            SettingsPage.OUTSIDE_LISTS -> Icons.Outlined.Inbox
            SettingsPage.MOVE -> Icons.Outlined.UploadFile
            SettingsPage.DOWNLOADERS -> Icons.Outlined.Download
            SettingsPage.SOURCES -> Icons.Outlined.Source
            SettingsPage.PICK_RULES -> Icons.Outlined.Star
            SettingsPage.SEARCH -> Icons.Outlined.Search
            SettingsPage.SCHEDULES -> Icons.Outlined.Schedule
            SettingsPage.USERS -> Icons.Outlined.Group
            SettingsPage.REQUESTS -> Icons.Outlined.Checklist
            SettingsPage.SIGN_IN -> Icons.Outlined.Shield
            SettingsPage.APPEARANCE -> Icons.Outlined.Palette
            SettingsPage.MEDIA_SERVERS -> Icons.Outlined.Dns
            SettingsPage.ALERTS -> Icons.Outlined.Notifications
            SettingsPage.AI -> Icons.Outlined.AutoAwesome
            SettingsPage.API -> Icons.Outlined.Key
            SettingsPage.REMOTE_ACCESS -> Icons.Outlined.Public
            SettingsPage.METADATA -> Icons.Outlined.Language
            SettingsPage.HEALTH -> Icons.Outlined.TaskAlt
            SettingsPage.BACKUPS -> Icons.Outlined.Archive
            SettingsPage.LOGS -> Icons.Outlined.Description
            SettingsPage.THIS_APP -> Icons.Outlined.Info
            SettingsPage.DANGER -> Icons.Outlined.Warning
            SettingsPage.ACCOUNT -> Icons.Outlined.AccountCircle
        }

@StringRes
fun SettingsGroup.titleRes(): Int =
    when (this) {
        SettingsGroup.LIBRARY -> R.string.settings_group_library
        SettingsGroup.DOWNLOADS -> R.string.settings_group_downloads
        SettingsGroup.USERS -> R.string.settings_group_users
        SettingsGroup.CONNECTIONS -> R.string.settings_group_connections
        SettingsGroup.SYSTEM -> R.string.settings_group_system
        SettingsGroup.ACCOUNT -> R.string.settings_group_account
    }

@StringRes
fun SettingsGroup.lineRes(): Int =
    when (this) {
        SettingsGroup.LIBRARY -> R.string.settings_group_library_line
        SettingsGroup.DOWNLOADS -> R.string.settings_group_downloads_line
        SettingsGroup.USERS -> R.string.settings_group_users_line
        SettingsGroup.CONNECTIONS -> R.string.settings_group_connections_line
        SettingsGroup.SYSTEM -> R.string.settings_group_system_line
        SettingsGroup.ACCOUNT -> R.string.settings_group_account_line
    }
