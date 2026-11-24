package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.AddOutcome
import ch.orellbuehler.acolyte.api.model.AddTorrentsBody
import ch.orellbuehler.acolyte.api.model.BulkBody
import ch.orellbuehler.acolyte.api.model.CalendarAnswer
import ch.orellbuehler.acolyte.api.model.ClientViews
import ch.orellbuehler.acolyte.api.model.DecideAnswer
import ch.orellbuehler.acolyte.api.model.DecideBody
import ch.orellbuehler.acolyte.api.model.DecisionList
import ch.orellbuehler.acolyte.api.model.DeleteFilesBody
import ch.orellbuehler.acolyte.api.model.DlOk
import ch.orellbuehler.acolyte.api.model.DownloadDetail
import ch.orellbuehler.acolyte.api.model.DownloaderList
import ch.orellbuehler.acolyte.api.model.DownloaderSettings
import ch.orellbuehler.acolyte.api.model.DownloadsView
import ch.orellbuehler.acolyte.api.model.EditionAnswer
import ch.orellbuehler.acolyte.api.model.EditionBody
import ch.orellbuehler.acolyte.api.model.EventsPage
import ch.orellbuehler.acolyte.api.model.FilePriorityBody
import ch.orellbuehler.acolyte.api.model.FollowAnswer
import ch.orellbuehler.acolyte.api.model.ImportList
import ch.orellbuehler.acolyte.api.model.LiveInterestAnswer
import ch.orellbuehler.acolyte.api.model.LiveInterestBody
import ch.orellbuehler.acolyte.api.model.LocationBody
import ch.orellbuehler.acolyte.api.model.MatchList
import ch.orellbuehler.acolyte.api.model.MatchPickBody
import ch.orellbuehler.acolyte.api.model.MatchRunAnswer
import ch.orellbuehler.acolyte.api.model.MatchView
import ch.orellbuehler.acolyte.api.model.MovieOverview
import ch.orellbuehler.acolyte.api.model.PersonList
import ch.orellbuehler.acolyte.api.model.PriorityBody
import ch.orellbuehler.acolyte.api.model.ReleaseRef
import ch.orellbuehler.acolyte.api.model.RemoveAnswer
import ch.orellbuehler.acolyte.api.model.RemoveBody
import ch.orellbuehler.acolyte.api.model.SearchAnswer
import ch.orellbuehler.acolyte.api.model.SearchBody
import ch.orellbuehler.acolyte.api.model.SeasonEpisodes
import ch.orellbuehler.acolyte.api.model.ShareLimitsBody
import ch.orellbuehler.acolyte.api.model.SlowModeAnswer
import ch.orellbuehler.acolyte.api.model.SlowModeBody
import ch.orellbuehler.acolyte.api.model.SourceList
import ch.orellbuehler.acolyte.api.model.SpeedLimitBody
import ch.orellbuehler.acolyte.api.model.Swarm
import ch.orellbuehler.acolyte.api.model.TitleBriefList
import ch.orellbuehler.acolyte.api.model.TitleRef
import ch.orellbuehler.acolyte.api.model.TorrentFiles
import ch.orellbuehler.acolyte.api.model.TorrentSpeedBody
import ch.orellbuehler.acolyte.api.model.TrackerAddBody
import ch.orellbuehler.acolyte.api.model.TrackerEditBody
import ch.orellbuehler.acolyte.api.model.TrackerRemoveBody
import ch.orellbuehler.acolyte.api.model.TurnDownBody
import ch.orellbuehler.acolyte.api.model.UsenetView

internal data object DownloadsEndpoints : EndpointGroup() {
    private const val OWN = "/api/v1/downloads/{id}"
    private const val ITEM = "/api/v1/downloaders/{id}/items/{hash}"
    private const val CLIENT = "/api/v1/downloaders/{id}"
    private val ok = DlOk.serializer()
    private val m = Need.MANAGE_LIBRARY

    val view =
        get(
            "/api/v1/downloads/view",
            DownloadsView.serializer(),
            Need.SIGNED_IN,
            query = listOf("limit", "client", "outside_limit", "cursor"),
        )
    val imports = get("/api/v1/imports", ImportList.serializer(), m, query = listOf("limit"))
    val clients = get("/api/v1/downloads/clients", ClientViews.serializer(), m)
    val pauseAll = post("/api/v1/downloads/pause-all", ClientViews.serializer(), m)
    val resumeAll = post("/api/v1/downloads/resume-all", ClientViews.serializer(), m)

    val pause = post("$OWN/pause", ok, m)
    val resume = post("$OWN/resume", ok, m)
    val recheck = post("$OWN/recheck", ok, m)
    val reannounce = post("$OWN/reannounce", ok, m)
    val remove = post("$OWN/remove", RemoveBody.serializer(), RemoveAnswer.serializer(), m)
    val markFailed = post("$OWN/mark-failed", RemoveAnswer.serializer(), m)
    val importAgain = post("$OWN/import", ok, m)
    val edition = put("$OWN/edition", EditionBody.serializer(), EditionAnswer.serializer(), m, Part.MEDIA)
    val detail = get("$OWN/detail", DownloadDetail.serializer(), m)
    val files = get("$OWN/files", TorrentFiles.serializer(), m)
    val setFiles = put("$OWN/files", FilePriorityBody.serializer(), ok, m)
    val swarm = get("$OWN/swarm", Swarm.serializer(), m)
    val usenet = get("$OWN/usenet", UsenetView.serializer(), m)

    val itemPause = post("$ITEM/pause", ok, m)
    val itemResume = post("$ITEM/resume", ok, m)
    val itemTop = post("$ITEM/top", ok, m)
    val itemUp = post("$ITEM/up", ok, m)
    val itemDown = post("$ITEM/down", ok, m)
    val itemBottom = post("$ITEM/bottom", ok, m)
    val itemRecheck = post("$ITEM/recheck", ok, m)
    val itemReannounce = post("$ITEM/reannounce", ok, m)
    val itemPriority = post("$ITEM/priority", PriorityBody.serializer(), ok, m)
    val itemRemove = post("$ITEM/remove", DeleteFilesBody.serializer(), ok, m)
    val itemRetry = post("$ITEM/retry", ok, m)
    val itemDetail = get("$ITEM/detail", DownloadDetail.serializer(), m)
    val itemFiles = get("$ITEM/files", TorrentFiles.serializer(), m)
    val itemSetFiles = put("$ITEM/files", FilePriorityBody.serializer(), ok, m)
    val itemSwarm = get("$ITEM/swarm", Swarm.serializer(), m)
    val itemUsenet = get("$ITEM/usenet", UsenetView.serializer(), m)
    val addTrackers = post("$ITEM/trackers", TrackerAddBody.serializer(), ok, m)
    val editTracker = put("$ITEM/trackers", TrackerEditBody.serializer(), ok, m)
    val removeTrackers = post("$ITEM/trackers/remove", TrackerRemoveBody.serializer(), ok, m)

    val bulk = post("$CLIENT/bulk", BulkBody.serializer(), ok, m)
    val shareLimits = post("$CLIENT/share-limits", ShareLimitsBody.serializer(), ok, m)
    val location = post("$CLIENT/location", LocationBody.serializer(), ok, m)
    val torrentSpeeds = post("$CLIENT/speed-limits", TorrentSpeedBody.serializer(), ok, m)
    val speedLimit = put("$CLIENT/speed-limit", SpeedLimitBody.serializer(), ok, m)
    val slowMode = post("$CLIENT/slow-mode", SlowModeBody.serializer(), SlowModeAnswer.serializer(), m)
    val addTorrents = post("$CLIENT/torrents", AddTorrentsBody.serializer(), AddOutcome.serializer(), m)

    val decisions = get("/api/v1/needs-decision", DecisionList.serializer(), m, Part.MEDIA)
    val decide = post("/api/v1/needs-decision/{id}/decide", DecideBody.serializer(), DecideAnswer.serializer(), m, Part.MEDIA)

    val matches = get("/api/v1/downloads/matches", MatchList.serializer(), m)
    val runMatches = post("/api/v1/downloads/matches/run", MatchRunAnswer.serializer(), m)
    val pickMatch = post("/api/v1/downloads/matches/{id}/pick", MatchPickBody.serializer(), MatchView.serializer(), m)

    val events =
        get(
            "/api/v1/events",
            EventsPage.serializer(),
            m,
            query = listOf("title", "episode", "kind", "from", "to", "source", "feed", "limit", "offset"),
        )
    val calendar = get("/api/v1/calendar", CalendarAnswer.serializer(), Need.SIGNED_IN, Part.MEDIA, query = listOf("from", "to"))
    val liveInterest = post("/api/v1/live/interest", LiveInterestBody.serializer(), LiveInterestAnswer.serializer(), Need.SIGNED_IN)
}

internal data object DownloadsLinkEndpoints : EndpointGroup() {
    val titlesBrief = get("/api/v1/titles", TitleBriefList.serializer(), Need.SIGNED_IN, Part.MEDIA, query = listOf("brief"))
    val title = get("/api/v1/titles/{id}", TitleRef.serializer(), Need.SIGNED_IN, Part.TITLE)
    val follow = put("/api/v1/titles/{id}/follow", FollowAnswer.serializer(), Need.SIGNED_IN, Part.WISHLIST)
    val unfollow = delete("/api/v1/titles/{id}/follow", FollowAnswer.serializer(), Need.SIGNED_IN, Part.WISHLIST)
    val search = post("/api/v1/titles/{id}/search", SearchBody.serializer(), SearchAnswer.serializer(), Need.MANAGE_LIBRARY, Part.TITLE)
    val turnDown =
        post("/api/v1/releases/{id}/turn-down", TurnDownBody.serializer(), ReleaseRef.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
    val season = get("/api/v1/metadata/show/{tmdb_id}/seasons/{number}", SeasonEpisodes.serializer(), Need.SIGNED_IN, Part.SHOWS)
    val metadata = get("/api/v1/metadata/{kind}/{tmdb_id}", MovieOverview.serializer(), Need.SIGNED_IN, Part.MEDIA)
    val downloaders = get("/api/v1/downloaders", DownloaderList.serializer(), Need.MANAGE_LIBRARY)
    val downloaderSettings = get("/api/v1/settings/downloaders", DownloaderSettings.serializer(), Need.SETTINGS)
    val users = get("/api/v1/users", PersonList.serializer(), Need.USERS)
    val sources = get("/api/v1/sources", SourceList.serializer(), Need.MANAGE_LIBRARY)
}
