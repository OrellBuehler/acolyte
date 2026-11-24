package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SettingsNamingRules(
    @SerialName("file_pattern") val filePattern: String = "",
    @SerialName("folder_pattern") val folderPattern: String = "",
    val colon: String = "",
    val illegal: String = "",
    @SerialName("rename_on_import") val renameOnImport: Boolean = false,
    @SerialName("group_by_genre") val groupByGenre: Boolean = false,
    @SerialName("group_by_letter") val groupByLetter: Boolean = false,
    @SerialName("show_group_by_letter") val showGroupByLetter: Boolean = false,
    @SerialName("show_rename_on_import") val showRenameOnImport: Boolean = false,
    @SerialName("show_colon") val showColon: String = "",
    @SerialName("show_illegal") val showIllegal: String = "",
    @SerialName("music_rename_on_import") val musicRenameOnImport: Boolean = false,
    @SerialName("music_colon") val musicColon: String = "",
    @SerialName("music_illegal") val musicIllegal: String = "",
    @SerialName("show_folder_pattern") val showFolderPattern: String = "",
    @SerialName("season_folders") val seasonFolders: Boolean = false,
    @SerialName("season_folder_pattern") val seasonFolderPattern: String = "",
    @SerialName("specials_folder") val specialsFolder: String = "",
    @SerialName("episode_pattern") val episodePattern: String = "",
    @SerialName("daily_pattern") val dailyPattern: String = "",
    @SerialName("anime_pattern") val animePattern: String = "",
    @SerialName("multi_episode_style") val multiEpisodeStyle: String = "",
    @SerialName("air_date_style") val airDateStyle: String = "",
    @SerialName("absolute_digits") val absoluteDigits: Int = 3,
    @SerialName("adopt_scan_limit") val adoptScanLimit: Int = 0,
    @SerialName("music_adopt_scan_limit") val musicAdoptScanLimit: Int = 0,
    @SerialName("artist_folder_pattern") val artistFolderPattern: String = "",
    @SerialName("album_folder_pattern") val albumFolderPattern: String = "",
    @SerialName("track_pattern") val trackPattern: String = "",
    @SerialName("multi_disc_track_pattern") val multiDiscTrackPattern: String = "",
    @SerialName("disc_folders") val discFolders: Boolean = false,
    @SerialName("disc_folder_pattern") val discFolderPattern: String = "",
    @SerialName("music_read_tags") val musicReadTags: Boolean = false,
    @SerialName("title_language") val titleLanguage: String = "",
)

@Serializable
data class SettingsNamingExampleBody(
    @SerialName("file_pattern") val filePattern: String = "",
    @SerialName("folder_pattern") val folderPattern: String = "",
    val colon: String = "",
    val illegal: String = "",
    @SerialName("rename_on_import") val renameOnImport: Boolean = false,
    @SerialName("group_by_genre") val groupByGenre: Boolean = false,
    @SerialName("group_by_letter") val groupByLetter: Boolean = false,
    @SerialName("show_group_by_letter") val showGroupByLetter: Boolean = false,
    @SerialName("show_colon") val showColon: String = "",
    @SerialName("show_illegal") val showIllegal: String = "",
    @SerialName("music_colon") val musicColon: String = "",
    @SerialName("music_illegal") val musicIllegal: String = "",
    @SerialName("title_language") val titleLanguage: String = "",
    @SerialName("show_folder_pattern") val showFolderPattern: String = "",
    @SerialName("season_folders") val seasonFolders: Boolean = false,
    @SerialName("season_folder_pattern") val seasonFolderPattern: String = "",
    @SerialName("specials_folder") val specialsFolder: String = "",
    @SerialName("episode_pattern") val episodePattern: String = "",
    @SerialName("daily_pattern") val dailyPattern: String = "",
    @SerialName("anime_pattern") val animePattern: String = "",
    @SerialName("multi_episode_style") val multiEpisodeStyle: String = "",
    @SerialName("air_date_style") val airDateStyle: String = "",
    @SerialName("absolute_digits") val absoluteDigits: Int = 3,
    @SerialName("artist_folder_pattern") val artistFolderPattern: String = "",
    @SerialName("album_folder_pattern") val albumFolderPattern: String = "",
    @SerialName("track_pattern") val trackPattern: String = "",
    @SerialName("multi_disc_track_pattern") val multiDiscTrackPattern: String = "",
    @SerialName("disc_folders") val discFolders: Boolean = false,
    @SerialName("disc_folder_pattern") val discFolderPattern: String = "",
)

@Serializable
data class SettingsNamingView(
    val naming: SettingsNamingRules = SettingsNamingRules(),
    val words: Map<String, String> = emptyMap(),
    val example: String = "",
    @SerialName("example_folder") val exampleFolder: String = "",
    @SerialName("example_episodes") val exampleEpisodes: Map<String, String> = emptyMap(),
    @SerialName("example_music") val exampleMusic: Map<String, String> = emptyMap(),
)

@Serializable
data class SettingsNamingExampleAnswer(
    val example: String = "",
    @SerialName("example_folder") val exampleFolder: String = "",
    @SerialName("example_episodes") val exampleEpisodes: Map<String, String> = emptyMap(),
    @SerialName("example_music") val exampleMusic: Map<String, String> = emptyMap(),
)

@Serializable
data class SettingsNamingCleanAnswer(
    @SerialName("job_id") val jobId: Long = 0,
)
