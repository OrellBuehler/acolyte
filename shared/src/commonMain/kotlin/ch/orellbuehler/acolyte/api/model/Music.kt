package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MusicPicture(
    val v: String = "",
    val w: Int = 0,
    val h: Int = 0,
    val color: String = "",
)

@Serializable
data class MusicPageAlbum(
    val mbid: String = "",
    val name: String = "",
    val type: String = "",
    @SerialName("secondary_types") val secondaryTypes: List<String> = emptyList(),
    @SerialName("release_date") val releaseDate: String = "",
    val disambiguation: String = "",
    val shelf: String = "",
    val image: String = "",
    val explicit: Boolean = false,
    val upcoming: Boolean = false,
    val state: String = "",
    @SerialName("part_id") val partId: Long = 0,
    val following: Boolean = false,
    @SerialName("artist_mbid") val artistMbid: String = "",
    @SerialName("artist_name") val artistName: String = "",
    val listeners: Long = 0,
)

@Serializable
data class MusicPageSong(
    val title: String = "",
    @SerialName("length_ms") val lengthMs: Long = 0,
    val explicit: Boolean = false,
    val album: String = "",
    @SerialName("album_mbid") val albumMbid: String = "",
    val image: String = "",
)

@Serializable
data class MusicStoreAlbum(
    val title: String = "",
    @SerialName("release_date") val releaseDate: String = "",
    val type: String = "",
    @SerialName("track_count") val trackCount: Int = 0,
    val explicit: Boolean = false,
    val image: String = "",
)

@Serializable
data class MusicPageArtist(
    val mbid: String = "",
    val name: String = "",
    val image: String = "",
    val note: String = "",
)

@Serializable
data class MusicWiki(
    val title: String = "",
    val extract: String = "",
    val url: String = "",
    val license: String = "",
    @SerialName("license_url") val licenseUrl: String = "",
    val attribution: String = "",
)

@Serializable
data class MusicPopularity(
    @SerialName("monthly_listeners") val monthlyListeners: Long = 0,
    @SerialName("world_rank") val worldRank: Int = 0,
    @SerialName("listeners_change_today") val listenersChangeToday: Long = 0,
    @SerialName("total_plays") val totalPlays: Long = 0,
)

@Serializable
data class MusicMissing(
    val count: Int = 0,
    @SerialName("size_bytes") val sizeBytes: Long = 0,
    val albums: List<String> = emptyList(),
)

@Serializable
data class MusicGap(
    val state: String = "",
    val position: Int = 0,
    val message: String = "",
)

@Serializable
data class MusicVideoItem(
    val id: String = "",
    val title: String = "",
    val duration: Double = 0.0,
)

@Serializable
data class MusicArtistAnswer(
    val mbid: String = "",
    val name: String = "",
    val type: String = "",
    val country: String = "",
    val disambiguation: String = "",
    val status: String = "",
    val genres: List<String> = emptyList(),
    val image: String = "",
    @SerialName("title_id") val titleId: Long = 0,
    val have: Int = 0,
    val total: Int = 0,
    val bio: MusicWiki? = null,
    val albums: List<MusicPageAlbum> = emptyList(),
    val top: List<MusicPageSong> = emptyList(),
    val popularity: MusicPopularity? = null,
    @SerialName("store_albums") val storeAlbums: List<MusicStoreAlbum> = emptyList(),
    val similar: List<MusicPageArtist> = emptyList(),
    val related: List<MusicPageArtist> = emptyList(),
    val kid: Boolean = false,
    val missing: MusicMissing = MusicMissing(),
    val incomplete: Boolean = false,
    val gap: MusicGap? = null,
    val pictures: Map<String, MusicPicture> = emptyMap(),
    val essential: List<String> = emptyList(),
    @SerialName("appears_on") val appearsOn: List<MusicPageAlbum> = emptyList(),
    val videos: List<MusicVideoItem> = emptyList(),
)

@Serializable
data class MusicPageTrack(
    val disc: Int = 0,
    val number: Int = 0,
    val name: String = "",
    @SerialName("length_ms") val lengthMs: Long = 0,
    val explicit: Boolean = false,
    val have: String = "",
)

@Serializable
data class MusicEdition(
    val mbid: String = "",
    val label: String = "",
    val tracks: Int = 0,
    val date: String = "",
    val list: List<MusicPageTrack> = emptyList(),
)

@Serializable
data class MusicAlbumAnswer(
    val mbid: String = "",
    val name: String = "",
    val type: String = "",
    @SerialName("secondary_types") val secondaryTypes: List<String> = emptyList(),
    @SerialName("release_date") val releaseDate: String = "",
    val disambiguation: String = "",
    val image: String = "",
    val explicit: Boolean = false,
    val state: String = "",
    @SerialName("part_id") val partId: Long = 0,
    val artist: MusicPageArtist? = null,
    @SerialName("title_id") val titleId: Long = 0,
    val label: String = "",
    @SerialName("length_ms") val lengthMs: Long = 0,
    val tracks: List<MusicPageTrack> = emptyList(),
    val editions: List<MusicEdition> = emptyList(),
    @SerialName("more_by") val moreBy: List<MusicPageAlbum> = emptyList(),
    val similar: List<MusicPageArtist> = emptyList(),
    val near: List<MusicPageAlbum> = emptyList(),
    val kid: Boolean = false,
    val incomplete: Boolean = false,
    val gap: MusicGap? = null,
    val pictures: Map<String, MusicPicture> = emptyMap(),
)

@Serializable
data class MusicFresh(
    val kind: String = "",
    val text: String = "",
    val rank: Int = 0,
)

@Serializable
data class MusicCard(
    val key: String = "",
    val kind: String = "",
    val mbid: String = "",
    val name: String = "",
    val artist: String = "",
    @SerialName("artist_mbid") val artistMbid: String = "",
    @SerialName("title_id") val titleId: Long = 0,
    @SerialName("part_id") val partId: Long = 0,
    val image: String = "",
    @SerialName("artist_image") val artistImage: String = "",
    val date: String = "",
    val state: String = "",
    val rank: Int = 0,
    @SerialName("listen_count") val listenCount: Long = 0,
    val upcoming: Boolean = false,
    val genres: List<String> = emptyList(),
    val note: String = "",
    val fresh: MusicFresh? = null,
    val badge: String = "",
    val kicker: String = "",
    val following: Boolean = false,
    val explicit: Boolean = false,
    val track: String = "",
    val hue: Int = 0,
    val images: List<String> = emptyList(),
    val cover: String = "",
    val href: String = "",
)

@Serializable
data class MusicRow(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val reason: String = "",
    val shape: String = "",
    val items: List<MusicCard> = emptyList(),
)

@Serializable
data class MusicSimilarLink(
    val key: String = "",
    val name: String = "",
    val href: String = "",
)

@Serializable
data class MusicExploreAnswer(
    val pictures: Map<String, MusicPicture> = emptyMap(),
    val kid: Boolean = false,
    val cold: Boolean = false,
    val partial: Boolean = false,
    val genre: String = "",
    val region: String = "",
    val regions: List<String> = emptyList(),
    val tile: MusicCard? = null,
    val hero: MusicCard? = null,
    @SerialName("hero_alts") val heroAlts: List<MusicCard> = emptyList(),
    val rows: List<MusicRow> = emptyList(),
    val next: String = "",
    val total: Int = 0,
    val thin: Boolean = false,
    val similar: List<MusicSimilarLink> = emptyList(),
)

@Serializable
data class MusicBrowseTile(
    val key: String = "",
    val name: String = "",
    val href: String = "",
    val hue: Int = 0,
    val covers: List<String> = emptyList(),
)

@Serializable
data class MusicBrowseSection(
    val id: String = "",
    val title: String = "",
    val tiles: List<MusicBrowseTile> = emptyList(),
)

@Serializable
data class MusicBrowseTiles(
    val kid: Boolean = false,
    val sections: List<MusicBrowseSection> = emptyList(),
    val explore: List<MusicBrowseTile> = emptyList(),
)

@Serializable
data class MusicNewAnswer(
    val kid: Boolean = false,
    val building: Boolean = false,
    val hero: List<MusicCard> = emptyList(),
    val rows: List<MusicRow> = emptyList(),
    val genres: List<String> = emptyList(),
)

@Serializable
internal data class MusicAlbumFollowBody(
    val name: String,
    val artist: String,
    @SerialName("release_date") val releaseDate: String,
)

@Serializable
data class MusicAlbumFollow(
    val mbid: String = "",
    val following: Boolean = false,
)

@Serializable
data class MusicSettings(
    @SerialName("album_choice") val albumChoice: String = "all",
    @SerialName("new_albums") val newAlbums: String = "all",
    @SerialName("album_types") val albumTypes: List<String> = listOf("album", "ep"),
    @SerialName("secondary_types") val secondaryTypes: List<String> = listOf("studio"),
    @SerialName("release_statuses") val releaseStatuses: List<String> = listOf("official"),
    @SerialName("pressing_rule") val pressingRule: String = "common",
    @SerialName("musicbrainz_url") val musicbrainzUrl: String = "",
    @SerialName("musicbrainz_allow_local_network") val musicbrainzAllowLocalNetwork: Boolean = false,
    @SerialName("cache_hours") val cacheHours: Int = 24,
)

@Serializable
internal data class AddArtistBody(
    val id: String,
    val follow: Boolean,
    @SerialName("library_folder_id") val libraryFolderId: Long,
    @SerialName("pick_rules_id") val pickRulesId: Long,
    @SerialName("keep_up_to_date") val keepUpToDate: Boolean,
    val albums: String,
    @SerialName("new_albums") val newAlbums: String,
    @SerialName("album_types") val albumTypes: List<String>,
    @SerialName("secondary_types") val secondaryTypes: List<String>,
    @SerialName("release_statuses") val releaseStatuses: List<String>,
)

@Serializable
data class MusicArtistAdded(
    val title: OwnedTitle? = null,
    @SerialName("job_id") val jobId: Long = 0,
)

@Serializable
internal data class ArtistChoicesBody(
    @SerialName("new_albums") val newAlbums: String? = null,
    @SerialName("album_types") val albumTypes: List<String>? = null,
    @SerialName("secondary_types") val secondaryTypes: List<String>? = null,
    @SerialName("release_statuses") val releaseStatuses: List<String>? = null,
)

@Serializable
internal data class AlbumChangeBody(
    @SerialName("keep_up_to_date") val keepUpToDate: Boolean? = null,
    @SerialName("pressing_id") val pressingId: Long? = null,
)

@Serializable
data class MusicAlbumSummary(
    val id: Long = 0,
    val mbid: String = "",
    val name: String = "",
    val type: String = "",
    @SerialName("secondary_types") val secondaryTypes: List<String> = emptyList(),
    val disambiguation: String = "",
    @SerialName("release_date") val releaseDate: String = "",
    val included: Boolean = false,
    @SerialName("keep_up_to_date") val keepUpToDate: Boolean = false,
    val dropped: Boolean = false,
    val pressings: Int = 0,
    val pressing: String = "",
    @SerialName("pressing_id") val pressingId: Long = 0,
    val tracks: Int = 0,
    @SerialName("in_library") val inLibrary: Int = 0,
    val missing: Int = 0,
    @SerialName("better_wanted") val betterWanted: Int = 0,
    @SerialName("not_out_yet") val notOutYet: Int = 0,
    val state: String = "",
)

@Serializable
data class MusicAlbumList(
    val albums: List<MusicAlbumSummary> = emptyList(),
)

@Serializable
data class MusicPressingTrack(
    val id: String = "",
    val disc: Int = 0,
    val number: Int = 0,
    val name: String = "",
    @SerialName("length_ms") val lengthMs: Long = 0,
)

@Serializable
data class MusicPressing(
    val id: Long = 0,
    val mbid: String = "",
    val name: String = "",
    val status: String = "",
    val country: String = "",
    val date: String = "",
    val disambiguation: String = "",
    val formats: List<String> = emptyList(),
    @SerialName("track_count") val trackCount: Int = 0,
    val chosen: Boolean = false,
    @SerialName("chosen_by") val chosenBy: String = "",
    val tracks: List<MusicPressingTrack> = emptyList(),
)

@Serializable
data class MusicAlbumDetail(
    val album: MusicAlbumSummary = MusicAlbumSummary(),
    @SerialName("title_id") val titleId: Long = 0,
    val pressings: List<MusicPressing> = emptyList(),
)

@Serializable
data class MusicTrackSlot(
    val id: Long = 0,
    val version: String = "",
    @SerialName("version_id") val versionId: Long = 0,
    val state: String = "",
    @SerialName("state_label") val stateLabel: String = "",
    val reason: String = "",
    @SerialName("file_id") val fileId: Long = 0,
)

@Serializable
data class MusicTrackView(
    val id: Long = 0,
    val disc: Int = 0,
    val number: Int = 0,
    val name: String = "",
    @SerialName("length_ms") val lengthMs: Long = 0,
    val dropped: Boolean = false,
    @SerialName("keep_up_to_date") val keepUpToDate: Boolean = false,
    val slots: List<MusicTrackSlot> = emptyList(),
)

@Serializable
data class MusicTrackList(
    @SerialName("album_id") val albumId: Long = 0,
    val tracks: List<MusicTrackView> = emptyList(),
)

@Serializable
data class MusicUnmatchedArtist(
    @SerialName("title_id") val titleId: Long = 0,
    val name: String = "",
    val files: Int = 0,
)

@Serializable
data class MusicUnmatched(
    val total: Int = 0,
    val artists: List<MusicUnmatchedArtist> = emptyList(),
)

@Serializable
data class MusicLeftoverMusic(
    @SerialName("album_id") val albumId: Long = 0,
    @SerialName("album_name") val albumName: String = "",
    @SerialName("track_id") val trackId: Long = 0,
    val owner: String = "",
)

@Serializable
data class MusicPressingHint(
    @SerialName("album_id") val albumId: Long = 0,
    @SerialName("album_name") val albumName: String = "",
    @SerialName("pressing_id") val pressingId: Long = 0,
    @SerialName("pressing_name") val pressingName: String = "",
    @SerialName("track_count") val trackCount: Int = 0,
    val fits: Int = 0,
    val chosen: Int = 0,
    val files: Int = 0,
)
