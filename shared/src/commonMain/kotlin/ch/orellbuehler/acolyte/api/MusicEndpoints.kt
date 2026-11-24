package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.AddArtistBody
import ch.orellbuehler.acolyte.api.model.AlbumChangeBody
import ch.orellbuehler.acolyte.api.model.ArtistChoicesBody
import ch.orellbuehler.acolyte.api.model.MusicAlbumAnswer
import ch.orellbuehler.acolyte.api.model.MusicAlbumDetail
import ch.orellbuehler.acolyte.api.model.MusicAlbumFollow
import ch.orellbuehler.acolyte.api.model.MusicAlbumFollowBody
import ch.orellbuehler.acolyte.api.model.MusicAlbumList
import ch.orellbuehler.acolyte.api.model.MusicAlbumSummary
import ch.orellbuehler.acolyte.api.model.MusicArtistAdded
import ch.orellbuehler.acolyte.api.model.MusicArtistAnswer
import ch.orellbuehler.acolyte.api.model.MusicBrowseTiles
import ch.orellbuehler.acolyte.api.model.MusicExploreAnswer
import ch.orellbuehler.acolyte.api.model.MusicNewAnswer
import ch.orellbuehler.acolyte.api.model.MusicRow
import ch.orellbuehler.acolyte.api.model.MusicSettings
import ch.orellbuehler.acolyte.api.model.MusicTrackList
import ch.orellbuehler.acolyte.api.model.MusicUnmatched

internal data object MusicEndpoints : EndpointGroup() {
    val artist =
        get("/api/v1/music/artists/{mbid}", MusicArtistAnswer.serializer(), Need.SIGNED_IN, Part.MUSIC, query = listOf("part", "n"))
    val album =
        get(
            "/api/v1/music/albums/{mbid}",
            MusicAlbumAnswer.serializer(),
            Need.SIGNED_IN,
            Part.MUSIC,
            query = listOf("part", "artist", "similar"),
        )
    val explore =
        get("/api/v1/explore/music", MusicExploreAnswer.serializer(), Need.SIGNED_IN, Part.MUSIC, query = listOf("region", "cursor"))
    val rising = get("/api/v1/explore/music/rising", MusicRow.serializer(), Need.SIGNED_IN, Part.MUSIC)
    val genre = get("/api/v1/music/genre/{slug}", MusicExploreAnswer.serializer(), Need.SIGNED_IN, Part.MUSIC, query = listOf("region"))
    val browse =
        get("/api/v1/music/browse/{kind}/{slug}", MusicExploreAnswer.serializer(), Need.SIGNED_IN, Part.MUSIC, query = listOf("region"))
    val tiles = get("/api/v1/music/browse-tiles", MusicBrowseTiles.serializer(), Need.SIGNED_IN, Part.MUSIC, query = listOf("region"))
    val newMusic = get("/api/v1/explore/new/music", MusicNewAnswer.serializer(), Need.SIGNED_IN, Part.MUSIC, query = listOf("region"))
    val followAlbum =
        put(
            "/api/v1/metadata/album/{mbid}/follow",
            MusicAlbumFollowBody.serializer(),
            MusicAlbumFollow.serializer(),
            Need.SIGNED_IN,
            Part.MUSIC,
        )
    val unfollowAlbum = delete("/api/v1/metadata/album/{mbid}/follow", MusicAlbumFollow.serializer(), Need.SIGNED_IN, Part.MUSIC)

    val settings = get("/api/v1/settings/music", MusicSettings.serializer(), Need.MANAGE_LIBRARY, Part.MUSIC)
    val saveSettings = put("/api/v1/settings/music", MusicSettings.serializer(), MusicSettings.serializer(), Need.SETTINGS, Part.MUSIC)
    val addArtist = post("/api/v1/artists", AddArtistBody.serializer(), MusicArtistAdded.serializer(), Need.MANAGE_LIBRARY, Part.MUSIC)
    val unmatched = get("/api/v1/library/unmatched-music", MusicUnmatched.serializer(), Need.MANAGE_LIBRARY, Part.MUSIC)

    val albums = get("/api/v1/titles/{id}/albums", MusicAlbumList.serializer(), Need.SIGNED_IN, Part.MUSIC, query = listOf("included"))
    val artistChoices =
        put("/api/v1/titles/{id}/artist", ArtistChoicesBody.serializer(), MusicArtistAdded.serializer(), Need.MANAGE_LIBRARY, Part.MUSIC)
    val libraryAlbum = get("/api/v1/albums/{id}", MusicAlbumDetail.serializer(), Need.SIGNED_IN, Part.MUSIC)
    val changeAlbum =
        put("/api/v1/albums/{id}", AlbumChangeBody.serializer(), MusicAlbumSummary.serializer(), Need.MANAGE_LIBRARY, Part.MUSIC)
    val albumTracks = get("/api/v1/albums/{id}/tracks", MusicTrackList.serializer(), Need.SIGNED_IN, Part.MUSIC)
}

internal object MusicPaths {
    fun wide(mbid: String): String = "/api/v1/music-trending/art/wide/$mbid"

    fun cover(albumId: Long): String = "/api/v1/albums/$albumId/cover"
}
