package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SettingsSearchValues(
    @SerialName("feed_check_minutes") val feedCheckMinutes: Int = 0,
    @SerialName("missing_search_count") val missingSearchCount: Int = 0,
    @SerialName("missing_search_better") val missingSearchBetter: Boolean = false,
    @SerialName("try_next_downloader") val tryNextDownloader: Boolean = false,
    @SerialName("missing_search_shows") val missingSearchShows: Boolean = false,
    @SerialName("season_pack_upgrade") val seasonPackUpgrade: String = "all",
    @SerialName("season_pack_prefer") val seasonPackPrefer: String = "rank",
    @SerialName("multi_season_packs") val multiSeasonPacks: Boolean = false,
    @SerialName("missing_search_music") val missingSearchMusic: Boolean = false,
    @SerialName("music_discography_packs") val musicDiscographyPacks: Boolean = false,
    @SerialName("album_track_tolerance") val albumTrackTolerance: Int = 0,
    @SerialName("album_extra_files") val albumExtraFiles: Int = 0,
    @SerialName("artist_search_cap") val artistSearchCap: Int = 200,
    @SerialName("refuse_early_releases") val refuseEarlyReleases: Boolean = false,
    @SerialName("early_release_grace_hours") val earlyReleaseGraceHours: Int = 24,
    @SerialName("refuse_tiny_releases") val refuseTinyReleases: Boolean = false,
    @SerialName("import_min_video_mb") val importMinVideoMb: Int = 0,
    @SerialName("import_min_track_kb") val importMinTrackKb: Int = 0,
    @SerialName("import_size_floors") val importSizeFloors: Boolean = false,
    @SerialName("hold_unknown_releases") val holdUnknownReleases: Boolean = false,
    @SerialName("episode_default_minutes") val episodeDefaultMinutes: Int = 20,
)
