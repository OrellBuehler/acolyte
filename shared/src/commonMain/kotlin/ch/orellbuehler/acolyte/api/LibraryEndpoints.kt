package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.AdoptRequest
import ch.orellbuehler.acolyte.api.model.AdoptScanRequest
import ch.orellbuehler.acolyte.api.model.AnyoneWatched
import ch.orellbuehler.acolyte.api.model.ArtistLookup
import ch.orellbuehler.acolyte.api.model.ByHandAnswer
import ch.orellbuehler.acolyte.api.model.ByHandImport
import ch.orellbuehler.acolyte.api.model.ByHandScan
import ch.orellbuehler.acolyte.api.model.ByHandScanRequest
import ch.orellbuehler.acolyte.api.model.CopySearchPreview
import ch.orellbuehler.acolyte.api.model.CopySearchRequest
import ch.orellbuehler.acolyte.api.model.FileSettings
import ch.orellbuehler.acolyte.api.model.FileSubtitleSearch
import ch.orellbuehler.acolyte.api.model.FolderBrowse
import ch.orellbuehler.acolyte.api.model.FolderBrowseRequest
import ch.orellbuehler.acolyte.api.model.FolderChange
import ch.orellbuehler.acolyte.api.model.FolderInput
import ch.orellbuehler.acolyte.api.model.FolderPath
import ch.orellbuehler.acolyte.api.model.HandsOff
import ch.orellbuehler.acolyte.api.model.HeldFiles
import ch.orellbuehler.acolyte.api.model.ImportFolders
import ch.orellbuehler.acolyte.api.model.ImportFoldersChange
import ch.orellbuehler.acolyte.api.model.JobIds
import ch.orellbuehler.acolyte.api.model.JobList
import ch.orellbuehler.acolyte.api.model.JobView
import ch.orellbuehler.acolyte.api.model.LibraryCopyPage
import ch.orellbuehler.acolyte.api.model.LibraryFolder
import ch.orellbuehler.acolyte.api.model.LibraryFolderList
import ch.orellbuehler.acolyte.api.model.LibrarySearch
import ch.orellbuehler.acolyte.api.model.LibraryStats
import ch.orellbuehler.acolyte.api.model.MissingSearch
import ch.orellbuehler.acolyte.api.model.MissingSubtitleSearch
import ch.orellbuehler.acolyte.api.model.MissingSubtitles
import ch.orellbuehler.acolyte.api.model.MovedTitle
import ch.orellbuehler.acolyte.api.model.MovieCollections
import ch.orellbuehler.acolyte.api.model.MovieLookup
import ch.orellbuehler.acolyte.api.model.MyWatched
import ch.orellbuehler.acolyte.api.model.OwnedEpisodes
import ch.orellbuehler.acolyte.api.model.OwnedTitle
import ch.orellbuehler.acolyte.api.model.PickRuleList
import ch.orellbuehler.acolyte.api.model.ReleasePage
import ch.orellbuehler.acolyte.api.model.RemovePlan
import ch.orellbuehler.acolyte.api.model.Removed
import ch.orellbuehler.acolyte.api.model.RenameApply
import ch.orellbuehler.acolyte.api.model.RenamePlan
import ch.orellbuehler.acolyte.api.model.RestoreAnswer
import ch.orellbuehler.acolyte.api.model.RestoreRequest
import ch.orellbuehler.acolyte.api.model.ScanStarted
import ch.orellbuehler.acolyte.api.model.SearchBody
import ch.orellbuehler.acolyte.api.model.ShowLookup
import ch.orellbuehler.acolyte.api.model.SideFiles
import ch.orellbuehler.acolyte.api.model.StartedJob
import ch.orellbuehler.acolyte.api.model.SubtitleLanguages
import ch.orellbuehler.acolyte.api.model.SubtitleSearchStarted
import ch.orellbuehler.acolyte.api.model.TagList
import ch.orellbuehler.acolyte.api.model.TidyApply
import ch.orellbuehler.acolyte.api.model.TidyPlan
import ch.orellbuehler.acolyte.api.model.TidyResult
import ch.orellbuehler.acolyte.api.model.TitleChange
import ch.orellbuehler.acolyte.api.model.TitleChanged
import ch.orellbuehler.acolyte.api.model.TitleGrid
import ch.orellbuehler.acolyte.api.model.TitleMove
import ch.orellbuehler.acolyte.api.model.TitleRefresh
import ch.orellbuehler.acolyte.api.model.TitleRemove
import ch.orellbuehler.acolyte.api.model.VersionAudioChange
import ch.orellbuehler.acolyte.api.model.WatchedCleanup
import ch.orellbuehler.acolyte.api.model.WatchedSummary
import kotlinx.serialization.json.JsonObject

internal data object LibraryEndpoints : EndpointGroup() {
    val titles = get("/api/v1/titles", TitleGrid.serializer(), Need.SIGNED_IN, Part.MEDIA, query = listOf("grid", "brief", "q"))
    val watched = get("/api/v1/my/watched", MyWatched.serializer(), Need.SIGNED_IN, Part.MEDIA)
    val watchedByAnyone = get("/api/v1/library/watched-by-anyone", AnyoneWatched.serializer(), Need.USERS, Part.MEDIA)
    val views = put("/api/v1/my/views", JsonObject.serializer(), JsonObject.serializer(), Need.SIGNED_IN)
    val collections = get("/api/v1/collections", MovieCollections.serializer(), Need.SIGNED_IN, Part.MOVIES)
    val pickRules = get("/api/v1/pick-rules", PickRuleList.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
    val change = put("/api/v1/titles/{id}", TitleChange.serializer(), TitleChanged.serializer(), Need.MANAGE_LIBRARY, Part.TITLE)
    val audio =
        put(
            "/api/v1/titles/{id}/versions/{vid}",
            VersionAudioChange.serializer(),
            JsonObject.serializer(),
            Need.MANAGE_LIBRARY,
            Part.TITLE,
        )
    val search = post("/api/v1/titles/{id}/search", SearchBody.serializer(), JobIds.serializer(), Need.MANAGE_LIBRARY, Part.TITLE)
    val refresh = post("/api/v1/titles/refresh", TitleRefresh.serializer(), JobIds.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
    val move = post("/api/v1/titles/{id}/move", TitleMove.serializer(), MovedTitle.serializer(), Need.MANAGE_LIBRARY, Part.TITLE)
    val remove = post("/api/v1/titles/{id}/delete", TitleRemove.serializer(), RemovePlan.serializer(), Need.MANAGE_LIBRARY, Part.TITLE)
    val searchMissing = post("/api/v1/search/missing", MissingSearch.serializer(), JobIds.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)

    val holding = get("/api/v1/holding", HeldFiles.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA, query = listOf("title_id", "count"))
    val restore = post("/api/v1/holding/restore", RestoreRequest.serializer(), RestoreAnswer.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
    val fileSettings = get("/api/v1/settings/files", FileSettings.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)

    val held = get("/api/v1/held", ReleasePage.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA, query = listOf("limit", "offset"))
    val unhold = delete("/api/v1/held/{id}", Removed.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
    val unholdAll = delete("/api/v1/held", Removed.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
    val turnedDown =
        get("/api/v1/turned-down", ReleasePage.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA, query = listOf("limit", "offset"))
    val takeOff = delete("/api/v1/turned-down/{id}", Removed.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
    val takeOffAll = delete("/api/v1/turned-down", Removed.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)

    val copies =
        get(
            "/api/v1/library/copies",
            LibraryCopyPage.serializer(),
            Need.MANAGE_LIBRARY,
            Part.MOVIES,
            query = listOf("q", "codec", "sort", "limit", "offset"),
        )
    val copiesPreview =
        post(
            "/api/v1/library/copies/search/preview",
            CopySearchRequest.serializer(),
            CopySearchPreview.serializer(),
            Need.MANAGE_LIBRARY,
            Part.MOVIES,
        )
    val copiesSearch =
        post("/api/v1/library/copies/search", CopySearchRequest.serializer(), JobIds.serializer(), Need.MANAGE_LIBRARY, Part.MOVIES)

    val missingSubtitles =
        get(
            "/api/v1/subtitles/missing",
            MissingSubtitles.serializer(),
            Need.MANAGE_LIBRARY,
            Part.SUBTITLES,
            query = listOf("language", "kind", "title", "limit", "offset"),
        )
    val searchMissingSubtitles =
        post(
            "/api/v1/subtitles/missing/search",
            MissingSubtitleSearch.serializer(),
            SubtitleSearchStarted.serializer(),
            Need.MANAGE_LIBRARY,
            Part.SUBTITLES,
        )
    val searchFileSubtitles =
        post(
            "/api/v1/subtitles/search",
            FileSubtitleSearch.serializer(),
            SubtitleSearchStarted.serializer(),
            Need.MANAGE_LIBRARY,
            Part.SUBTITLES,
        )
    val subtitleLanguages = get("/api/v1/subtitles/languages", SubtitleLanguages.serializer(), Need.MANAGE_LIBRARY, Part.SUBTITLES)

    val renamePlan = get("/api/v1/rename", RenamePlan.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
    val rename = post("/api/v1/rename", RenameApply.serializer(), StartedJob.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
    val job = get("/api/v1/jobs/{id}", JobView.serializer(), Need.MANAGE_LIBRARY)
    val jobs = get("/api/v1/jobs", JobList.serializer(), Need.MANAGE_LIBRARY, query = listOf("limit"))

    val stats = get("/api/v1/library/stats", LibraryStats.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
    val statsWatched = get("/api/v1/library/stats/watched", WatchedSummary.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
    val watchedCleanup =
        get("/api/v1/library/watched-cleanup", WatchedCleanup.serializer(), Need.USERS, Part.MEDIA, query = listOf("days", "people"))

    val folders = get("/api/v1/library-folders", LibraryFolderList.serializer(), Need.MANAGE_LIBRARY)
    val addFolder = post("/api/v1/library-folders", FolderInput.serializer(), LibraryFolder.serializer(), Need.SETTINGS)
    val changeFolder = put("/api/v1/library-folders/{id}", FolderChange.serializer(), LibraryFolder.serializer(), Need.SETTINGS)
    val removeFolder = delete("/api/v1/library-folders/{id}", NoContent, Need.SETTINGS)
    val moveFolder = put("/api/v1/library-folders/{id}/path", FolderPath.serializer(), LibraryFolder.serializer(), Need.SETTINGS)
    val checkFolder = post("/api/v1/library-folders/{id}/check", LibraryFolder.serializer(), Need.MANAGE_LIBRARY)
    val importFolders = get("/api/v1/settings/import-folders", ImportFolders.serializer(), Need.SETTINGS, Part.MEDIA)
    val saveImportFolders =
        put("/api/v1/settings/import-folders", ImportFoldersChange.serializer(), ImportFolders.serializer(), Need.USERS, Part.MEDIA)
    val sideFiles = get("/api/v1/settings/side-files", SideFiles.serializer(), Need.SETTINGS, Part.MEDIA)
    val saveSideFiles = put("/api/v1/settings/side-files", SideFiles.serializer(), SideFiles.serializer(), Need.SETTINGS, Part.MEDIA)
    val handsOff = put("/api/v1/settings/hands-off", HandsOff.serializer(), HandsOff.serializer(), Need.OWNER)
    val tags = get("/api/v1/tags", TagList.serializer(), Need.MANAGE_LIBRARY)

    val scan =
        post("/api/v1/library-folders/{id}/scan", AdoptScanRequest.serializer(), ScanStarted.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
    val adopt =
        post("/api/v1/library-folders/{id}/adopt", AdoptRequest.serializer(), StartedJob.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
    val tidyPlan = get("/api/v1/library-folders/{id}/tidy", TidyPlan.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
    val tidy = post("/api/v1/library-folders/{id}/tidy", TidyApply.serializer(), TidyResult.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
    val untidy = post("/api/v1/library-folders/{id}/tidy/undo", TidyResult.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
    val lookupMovies = get("/api/v1/titles/lookup", MovieLookup.serializer(), Need.SIGNED_IN, Part.MOVIES, query = listOf("term", "year"))
    val lookupShows = get("/api/v1/shows/lookup", ShowLookup.serializer(), Need.SIGNED_IN, Part.SHOWS, query = listOf("term"))
    val lookupArtists = get("/api/v1/artists/lookup", ArtistLookup.serializer(), Need.SIGNED_IN, Part.MUSIC, query = listOf("term"))

    val byHandScan =
        post("/api/v1/manual-import/scan", ByHandScanRequest.serializer(), ByHandScan.serializer(), Need.SETTINGS, Part.MEDIA)
    val byHandImport = post("/api/v1/manual-import", ByHandImport.serializer(), ByHandAnswer.serializer(), Need.SETTINGS, Part.MEDIA)
    val browse =
        post("/api/v1/manual-import/folders", FolderBrowseRequest.serializer(), FolderBrowse.serializer(), Need.USERS, Part.MEDIA)
    val librarySearch = get("/api/v1/library/search", LibrarySearch.serializer(), Need.SIGNED_IN, Part.MEDIA, query = listOf("q", "kind"))
    val titleVersions = get("/api/v1/titles/{id}", OwnedTitle.serializer(), Need.SIGNED_IN, Part.TITLE)
    val episodes =
        get(
            "/api/v1/titles/{id}/episodes",
            OwnedEpisodes.serializer(),
            Need.SIGNED_IN,
            Part.SHOWS,
            query = listOf("season", "files", "points"),
        )
}
