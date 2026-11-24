package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.AddMovieBody
import ch.orellbuehler.acolyte.api.model.AddShowBody
import ch.orellbuehler.acolyte.api.model.AdoptBody
import ch.orellbuehler.acolyte.api.model.CheckPickBody
import ch.orellbuehler.acolyte.api.model.ChooseBody
import ch.orellbuehler.acolyte.api.model.DismissBody
import ch.orellbuehler.acolyte.api.model.FileChoice
import ch.orellbuehler.acolyte.api.model.FilePoints
import ch.orellbuehler.acolyte.api.model.KeepBody
import ch.orellbuehler.acolyte.api.model.MatchBody
import ch.orellbuehler.acolyte.api.model.OwnedEpisode
import ch.orellbuehler.acolyte.api.model.OwnedEpisodeGroups
import ch.orellbuehler.acolyte.api.model.OwnedFile
import ch.orellbuehler.acolyte.api.model.OwnedSeason
import ch.orellbuehler.acolyte.api.model.OwnedVersion
import ch.orellbuehler.acolyte.api.model.PackNext
import ch.orellbuehler.acolyte.api.model.ReleaseCheckBody
import ch.orellbuehler.acolyte.api.model.RenameBody
import ch.orellbuehler.acolyte.api.model.SubtitleChoices
import ch.orellbuehler.acolyte.api.model.SubtitleChooseBody
import ch.orellbuehler.acolyte.api.model.SubtitleCleaned
import ch.orellbuehler.acolyte.api.model.SubtitleReasonBody
import ch.orellbuehler.acolyte.api.model.SubtitleRowView
import ch.orellbuehler.acolyte.api.model.SubtitleSaved
import ch.orellbuehler.acolyte.api.model.SubtitleSyncBody
import ch.orellbuehler.acolyte.api.model.SubtitleTranslateBody
import ch.orellbuehler.acolyte.api.model.SubtitleTranslating
import ch.orellbuehler.acolyte.api.model.SubtitleWishBody
import ch.orellbuehler.acolyte.api.model.SubtitleWishes
import ch.orellbuehler.acolyte.api.model.SubtitlesCleaned
import ch.orellbuehler.acolyte.api.model.TitleAdded
import ch.orellbuehler.acolyte.api.model.TitleAnime
import ch.orellbuehler.acolyte.api.model.TitleChoice
import ch.orellbuehler.acolyte.api.model.TitleDetails
import ch.orellbuehler.acolyte.api.model.TitleDismissed
import ch.orellbuehler.acolyte.api.model.TitleExtras
import ch.orellbuehler.acolyte.api.model.TitleFollow
import ch.orellbuehler.acolyte.api.model.TitleJob
import ch.orellbuehler.acolyte.api.model.TitleJobIds
import ch.orellbuehler.acolyte.api.model.TitleLeftovers
import ch.orellbuehler.acolyte.api.model.TitleMatched
import ch.orellbuehler.acolyte.api.model.TitleMetaFollow
import ch.orellbuehler.acolyte.api.model.TitleMoreFrom
import ch.orellbuehler.acolyte.api.model.TitlePeople
import ch.orellbuehler.acolyte.api.model.TitleRelease
import ch.orellbuehler.acolyte.api.model.TitleReleasePage
import ch.orellbuehler.acolyte.api.model.TitleRenamePlan
import ch.orellbuehler.acolyte.api.model.TitleReviews
import ch.orellbuehler.acolyte.api.model.TitleSearchOutcome
import ch.orellbuehler.acolyte.api.model.TitleSeasonEpisodes
import ch.orellbuehler.acolyte.api.model.TitleSubtitles
import ch.orellbuehler.acolyte.api.model.TitleTrailerStream
import ch.orellbuehler.acolyte.api.model.TitleUnmatched
import ch.orellbuehler.acolyte.api.model.TitleWatchLinks
import ch.orellbuehler.acolyte.api.model.TitleWiki
import ch.orellbuehler.acolyte.api.model.TurnDownBody
import ch.orellbuehler.acolyte.api.model.UnmatchBody
import ch.orellbuehler.acolyte.api.model.VersionInput

internal data object TitleEndpoints : EndpointGroup() {
    val details =
        get("/api/v1/metadata/{kind}/{tmdb_id}", TitleDetails.serializer(), Need.SIGNED_IN, Part.MEDIA)
    val season =
        get("/api/v1/metadata/show/{tmdb_id}/seasons/{number}", TitleSeasonEpisodes.serializer(), Need.SIGNED_IN, Part.SHOWS)
    val anime = get("/api/v1/metadata/{kind}/{tmdb_id}/anime", TitleAnime.serializer(), Need.SIGNED_IN, Part.MEDIA)
    val moreFrom = get("/api/v1/metadata/{kind}/{tmdb_id}/more-from", TitleMoreFrom.serializer(), Need.SIGNED_IN, Part.MEDIA)
    val reviews =
        get(
            "/api/v1/metadata/{kind}/{tmdb_id}/reviews",
            TitleReviews.serializer(),
            Need.SIGNED_IN,
            Part.MEDIA,
            query = listOf("source", "page", "order"),
        )
    val extras = get("/api/v1/extras/{kind}/{id}", TitleExtras.serializer(), Need.SIGNED_IN, Part.MEDIA)
    val wiki = get("/api/v1/wiki/{kind}/{id}", TitleWiki.serializer(), Need.SIGNED_IN, Part.MOVIES)
    val trailer = get("/api/v1/trailer/{kind}/{tmdb_id}", TitleTrailerStream.serializer(), Need.SIGNED_IN, Part.MEDIA)

    val followMovie = get("/api/v1/metadata/movie/{tmdb_id}/follow", TitleMetaFollow.serializer(), Need.SIGNED_IN, Part.WISHLIST)
    val followMovieOn = put("/api/v1/metadata/movie/{tmdb_id}/follow", TitleMetaFollow.serializer(), Need.SIGNED_IN, Part.WISHLIST)
    val followMovieOff = delete("/api/v1/metadata/movie/{tmdb_id}/follow", TitleMetaFollow.serializer(), Need.SIGNED_IN, Part.WISHLIST)
    val followShow = get("/api/v1/metadata/show/{tmdb_id}/follow", TitleMetaFollow.serializer(), Need.SIGNED_IN, Part.WISHLIST)
    val followShowOn = put("/api/v1/metadata/show/{tmdb_id}/follow", TitleMetaFollow.serializer(), Need.SIGNED_IN, Part.WISHLIST)
    val followShowOff = delete("/api/v1/metadata/show/{tmdb_id}/follow", TitleMetaFollow.serializer(), Need.SIGNED_IN, Part.WISHLIST)
    val followTitle = get("/api/v1/titles/{id}/follow", TitleFollow.serializer(), Need.SIGNED_IN, Part.WISHLIST)
    val followTitleOn = put("/api/v1/titles/{id}/follow", TitleFollow.serializer(), Need.SIGNED_IN, Part.WISHLIST)
    val followTitleOff = delete("/api/v1/titles/{id}/follow", TitleFollow.serializer(), Need.SIGNED_IN, Part.WISHLIST)

    val people = get("/api/v1/titles/{id}/people", TitlePeople.serializer(), Need.SIGNED_IN, Part.TITLE)
    val outcome = get("/api/v1/titles/{id}/search-outcome", TitleSearchOutcome.serializer(), Need.SIGNED_IN, Part.TITLE)
    val watchLinks =
        get("/api/v1/media-servers/watch-links", TitleWatchLinks.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA, query = listOf("title"))
    val refresh = post("/api/v1/titles/{id}/refresh", TitleJobIds.serializer(), Need.MANAGE_LIBRARY, Part.TITLE)
    val keepSeason =
        put("/api/v1/titles/{id}/seasons/{number}", KeepBody.serializer(), OwnedSeason.serializer(), Need.MANAGE_LIBRARY, Part.SHOWS)
    val keepEpisode = put("/api/v1/episodes/{id}", KeepBody.serializer(), OwnedEpisode.serializer(), Need.MANAGE_LIBRARY, Part.SHOWS)
    val episodeGroups = get("/api/v1/titles/{id}/episode-groups", OwnedEpisodeGroups.serializer(), Need.MANAGE_LIBRARY, Part.SHOWS)
    val packNext = post("/api/v1/titles/{id}/seasons/{number}/pack-next", PackNext.serializer(), Need.MANAGE_LIBRARY, Part.SHOWS)
    val points = get("/api/v1/titles/{id}/points", FilePoints.serializer(), Need.MANAGE_LIBRARY, Part.TITLE, query = listOf("slot"))

    val addVersion =
        post("/api/v1/titles/{id}/versions", VersionInput.serializer(), OwnedVersion.serializer(), Need.MANAGE_LIBRARY, Part.TITLE)
    val removeVersion = delete("/api/v1/titles/{id}/versions/{vid}", NoContent, Need.MANAGE_LIBRARY, Part.TITLE)
    val adopt =
        post("/api/v1/titles/{id}/versions/{vid}/adopt", AdoptBody.serializer(), OwnedFile.serializer(), Need.MANAGE_LIBRARY, Part.TITLE)
    val fileChoices =
        put("/api/v1/titles/{id}/files/{fid}/choices", FileChoice.serializer(), OwnedFile.serializer(), Need.MANAGE_LIBRARY, Part.TITLE)
    val keepAndAdd =
        post(
            "/api/v1/titles/{id}/files/{fid}/keep-and-add",
            VersionInput.serializer(),
            OwnedVersion.serializer(),
            Need.MANAGE_LIBRARY,
            Part.TITLE,
        )

    val releases =
        get(
            "/api/v1/titles/{id}/releases",
            TitleReleasePage.serializer(),
            Need.MANAGE_LIBRARY,
            Part.TITLE,
            query = listOf("limit", "offset", "item_id", "season"),
        )
    val choose = post("/api/v1/releases/{id}/choose", ChooseBody.serializer(), TitleChoice.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
    val turnDown =
        post("/api/v1/releases/{id}/turn-down", TurnDownBody.serializer(), TitleRelease.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
    val check = post("/api/v1/releases/check", ReleaseCheckBody.serializer(), TitleJobIds.serializer(), Need.MANAGE_LIBRARY, Part.MOVIES)
    val checkResults = get("/api/v1/releases/check/{id}", TitleReleasePage.serializer(), Need.MANAGE_LIBRARY, Part.MOVIES)
    val checkPick =
        post("/api/v1/releases/check/{id}/pick", CheckPickBody.serializer(), TitleChoice.serializer(), Need.MANAGE_LIBRARY, Part.MOVIES)

    val addMovie = post("/api/v1/titles", AddMovieBody.serializer(), TitleAdded.serializer(), Need.MANAGE_LIBRARY, Part.MOVIES)
    val addShow = post("/api/v1/shows", AddShowBody.serializer(), TitleAdded.serializer(), Need.MANAGE_LIBRARY, Part.SHOWS)

    val subtitles = get("/api/v1/titles/{id}/subtitles", TitleSubtitles.serializer(), Need.MANAGE_LIBRARY, Part.SUBTITLES)
    val subtitleWish =
        put(
            "/api/v1/titles/{id}/subtitle-wish",
            SubtitleWishBody.serializer(),
            SubtitleWishes.serializer(),
            Need.MANAGE_LIBRARY,
            Part.SUBTITLES,
        )
    val subtitleChoices = post("/api/v1/files/{id}/subtitles/choices", SubtitleChoices.serializer(), Need.MANAGE_LIBRARY, Part.SUBTITLES)
    val subtitleChoose =
        post(
            "/api/v1/files/{id}/subtitles/choose",
            SubtitleChooseBody.serializer(),
            SubtitleSaved.serializer(),
            Need.MANAGE_LIBRARY,
            Part.SUBTITLES,
        )
    val subtitleTranslate =
        post(
            "/api/v1/files/{id}/subtitles/translate",
            SubtitleTranslateBody.serializer(),
            SubtitleTranslating.serializer(),
            Need.MANAGE_LIBRARY,
            Part.SUBTITLES,
        )
    val subtitleSync =
        post(
            "/api/v1/subtitles/{id}/sync",
            SubtitleSyncBody.serializer(),
            SubtitleRowView.serializer(),
            Need.MANAGE_LIBRARY,
            Part.SUBTITLES,
        )
    val subtitleClean = post("/api/v1/subtitles/{id}/clean", SubtitleCleaned.serializer(), Need.MANAGE_LIBRARY, Part.SUBTITLES)
    val subtitleTurnDown =
        post("/api/v1/subtitles/{id}/turn-down", SubtitleReasonBody.serializer(), NoContent, Need.MANAGE_LIBRARY, Part.SUBTITLES)
    val subtitlesClean = post("/api/v1/titles/{id}/subtitles/clean", SubtitlesCleaned.serializer(), Need.MANAGE_LIBRARY, Part.SUBTITLES)

    val leftovers = get("/api/v1/titles/{id}/leftovers", TitleLeftovers.serializer(), Need.MANAGE_LIBRARY, Part.TITLE)
    val match =
        post("/api/v1/titles/{id}/leftovers/match", MatchBody.serializer(), TitleMatched.serializer(), Need.MANAGE_LIBRARY, Part.TITLE)
    val unmatch =
        post(
            "/api/v1/titles/{id}/leftovers/unmatch",
            UnmatchBody.serializer(),
            TitleUnmatched.serializer(),
            Need.MANAGE_LIBRARY,
            Part.TITLE,
        )
    val dismiss =
        put("/api/v1/titles/{id}/leftovers/{fid}", DismissBody.serializer(), TitleDismissed.serializer(), Need.MANAGE_LIBRARY, Part.TITLE)
    val rescan = post("/api/v1/titles/{id}/rescan", TitleJob.serializer(), Need.MANAGE_LIBRARY, Part.TITLE)
    val renamePlan = get("/api/v1/titles/{id}/rename", TitleRenamePlan.serializer(), Need.MANAGE_LIBRARY, Part.TITLE)
    val rename = post("/api/v1/titles/{id}/rename", RenameBody.serializer(), TitleJob.serializer(), Need.MANAGE_LIBRARY, Part.TITLE)
}
