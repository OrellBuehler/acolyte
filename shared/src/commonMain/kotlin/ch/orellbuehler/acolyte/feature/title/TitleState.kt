package ch.orellbuehler.acolyte.feature.title

import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.Part
import ch.orellbuehler.acolyte.api.model.FilePoints
import ch.orellbuehler.acolyte.api.model.HeldFile
import ch.orellbuehler.acolyte.api.model.LibraryFolder
import ch.orellbuehler.acolyte.api.model.LookupMovie
import ch.orellbuehler.acolyte.api.model.LookupShow
import ch.orellbuehler.acolyte.api.model.OwnedEpisodeGroups
import ch.orellbuehler.acolyte.api.model.OwnedSlot
import ch.orellbuehler.acolyte.api.model.OwnedTitle
import ch.orellbuehler.acolyte.api.model.PickRule
import ch.orellbuehler.acolyte.api.model.TitleAsk
import ch.orellbuehler.acolyte.api.model.TitleCard
import ch.orellbuehler.acolyte.api.model.TitleCredit
import ch.orellbuehler.acolyte.api.model.TitleDetails
import ch.orellbuehler.acolyte.api.model.TitleEvent
import ch.orellbuehler.acolyte.api.model.TitleExtras
import ch.orellbuehler.acolyte.api.model.TitleHide
import ch.orellbuehler.acolyte.api.model.TitleMoreFrom
import ch.orellbuehler.acolyte.api.model.TitlePerson
import ch.orellbuehler.acolyte.api.model.TitleScore
import ch.orellbuehler.acolyte.api.model.TitleSearchOutcome
import ch.orellbuehler.acolyte.api.model.TitleUser
import ch.orellbuehler.acolyte.api.model.TitleVideo
import ch.orellbuehler.acolyte.api.model.TitleWatchLink
import ch.orellbuehler.acolyte.api.model.TitleWiki
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.session.Account

data class TitleAccess(
    val manage: Boolean = false,
    val mayAsk: Boolean = false,
    val mayHide: Boolean = false,
    val followOn: Boolean = false,
    val subtitlesOn: Boolean = false,
    val kindOn: Boolean = true,
    val handsOff: Boolean = false,
    val isKid: Boolean = false,
    val settings: Boolean = false,
) {
    internal companion object {
        fun of(
            account: Account?,
            kind: String,
        ): TitleAccess {
            account ?: return TitleAccess()
            val manage = account.allows(Need.MANAGE_LIBRARY, Part.CORE)
            return TitleAccess(
                manage = manage,
                mayAsk = account.allows(Need.ASK, Part.WISHLIST) && !manage,
                mayHide = account.can(Need.USERS),
                followOn = account.isOn(Part.WISHLIST),
                subtitlesOn = account.isOn(Part.SUBTITLES),
                kindOn = account.isOn(if (kind == "show") Part.SHOWS else Part.MOVIES),
                handsOff = account.handsOff,
                isKid = account.isKid,
                settings = account.can(Need.SETTINGS),
            )
        }
    }
}

enum class TitleTab {
    OVERVIEW,
    LIBRARY,
}

enum class TitleProblem {
    NONE,
    BLOCKED,
    NOT_FOUND,
    NOT_IN_LIBRARY,
}

enum class AskOffer {
    ASK,
    ASK_TOO,
    ASKED,
    SEASONS,
}

enum class HeroPill {
    NONE,
    HIDDEN,
    NEVER_ADD,
    ASKED,
    APPROVED,
    TURNED_DOWN,
    AVAILABLE,
}

enum class TitleAction {
    SEARCH,
    CHOOSE_RELEASE,
    RENAME,
    ADD_VERSION,
    SETTINGS,
    KEEP,
    STOP_KEEPING,
    REFRESH,
    HISTORY,
    DELETE,
    REPORT,
    HIDE,
}

enum class StatusSub {
    LEFT_IN_DOWNLOADER,
    DOWNLOADING_HANDS_OFF,
    FULL_SEASONS,
    HAVE_MOVIE,
    ON_ITS_WAY,
    LAST_TRY_FAILED,
    WAITS_FOR_COPY,
    NOT_KEPT,
}

enum class StatusButton {
    NONE,
    TRY_AGAIN,
    SEARCH_NOW,
}

enum class FactKind {
    NETWORK,
    STUDIO,
    RELEASED,
    RELEASED_YEAR,
    AIRED,
    RUNTIME,
    SEASONS,
    RATED,
    STATUS,
    ORIGIN,
    LANGUAGE,
    SPOKEN,
    ORIGINAL_TITLE,
    ALSO_KNOWN,
    DIGITAL,
    DISC,
    NEXT_EPISODE,
    BUDGET,
    REVENUE,
}

data class TitleFact(
    val kind: FactKind,
    val text: String = "",
    val number: Long = 0,
    val end: String = "",
    val season: Int = 0,
    val episode: Int = 0,
)

data class TitleLink(
    val name: String,
    val url: String,
)

data class HistoryRow(
    val event: TitleEvent,
    val tone: Tone,
)

data class SlotCard(
    val slot: OwnedSlot,
    val label: String,
    val main: Boolean,
    val badges: List<String>,
    val versionId: Long,
    val removable: Boolean,
)

data class TitleState(
    val kind: String,
    val tmdb: String,
    val libraryId: Long,
    val access: TitleAccess = TitleAccess(),
    val language: String = "en",
    val status: LoadStatus = LoadStatus(),
    val problem: TitleProblem = TitleProblem.NONE,
    val tab: TitleTab = TitleTab.OVERVIEW,
    val details: TitleDetails? = null,
    val movie: LookupMovie? = null,
    val show: LookupShow? = null,
    val owned: OwnedTitle? = null,
    val ownedError: ApiError? = null,
    val events: List<TitleEvent> = emptyList(),
    val extras: TitleExtras = TitleExtras(),
    val wiki: TitleWiki = TitleWiki(),
    val moreFrom: TitleMoreFrom = TitleMoreFrom(),
    val lead: List<TitleCard> = emptyList(),
    val following: Boolean = false,
    val followBusy: Boolean = false,
    val watchLinks: List<TitleWatchLink> = emptyList(),
    val outcome: TitleSearchOutcome? = null,
    val fourKOn: Boolean = false,
    val held: List<HeldFile> = emptyList(),
    val credits: List<TitleCredit> = emptyList(),
    val folders: List<LibraryFolder> = emptyList(),
    val rules: List<PickRule> = emptyList(),
    val episodeGroups: OwnedEpisodeGroups? = null,
    val choicesLoaded: Boolean = false,
    val choicesError: ApiError? = null,
    val renameCount: Int = 0,
    val leftoverCount: Int = 0,
    val points: Map<Long, FilePoints> = emptyMap(),
    val users: List<TitleUser> = emptyList(),
    val busy: String = "",
    val feedback: Feedback? = null,
) {
    val isShow: Boolean get() = kind == "show"

    val name: String get() =
        details?.name?.ifBlank { null } ?: movie?.name?.ifBlank { null } ?: show?.name?.ifBlank { null }
            ?: owned?.name.orEmpty()

    val year: Int get() =
        details?.year?.takeIf { it > 0 } ?: movie?.year?.takeIf { it > 0 } ?: show?.year?.takeIf { it > 0 } ?: owned?.year
            ?: 0

    val overview: String
        get() =
            details?.overview?.ifBlank { null } ?: movie?.overview?.ifBlank { null } ?: show?.overview?.ifBlank { null }
                ?: owned?.overview.orEmpty()

    val poster: String get() =
        details?.poster?.ifBlank { null } ?: movie?.poster?.ifBlank { null } ?: show?.poster?.ifBlank { null }
            ?: owned?.artwork?.firstOrNull().orEmpty()

    val backdrop: String get() = details?.backdrop?.ifBlank { null } ?: movie?.backdrop?.ifBlank { null } ?: owned?.backdrop.orEmpty()

    val logo: String
        get() {
            val lang = language.substringBefore('-')
            return details?.logos?.get(lang)?.ifBlank { null } ?: details?.logo?.ifBlank { null } ?: extras.logo.ifBlank { null }
                ?: owned?.logo.orEmpty()
        }

    val tagline: String get() = details?.tagline.orEmpty()

    val runtime: Int get() = details?.runtimeMinutes?.takeIf { it > 0 } ?: owned?.runtimeMinutes ?: 0

    val seasonCount: Int get() = details?.seasons.orEmpty().count { it.number > 0 }

    val genres: List<String> get() = details?.genres?.ifEmpty { null } ?: owned?.genres.orEmpty()

    val certification: String get() = details?.rating?.ifBlank { null } ?: owned?.rating.orEmpty()

    val ready: Boolean get() = status.phase == Phase.READY && problem == TitleProblem.NONE

    val isOwned: Boolean get() = owned != null

    val ownedId: Long get() = owned?.id ?: libraryId

    val hasTmdb: Boolean get() = tmdb.isNotEmpty()

    val legacy: Boolean get() = owned != null && !hasTmdb

    val neverAdd: Boolean get() = movie?.neverAdd ?: show?.neverAdd ?: details?.neverAdd ?: false

    val ask: TitleAsk? get() = movie?.ask ?: show?.ask ?: details?.ask

    val hidden: Boolean get() = (details?.hidden ?: false) || (movie?.hidden ?: false) || (show?.hidden ?: false)

    val hides: List<TitleHide> get() = details?.hides.orEmpty()

    val libraryStatus: LibraryStatus? get() = owned?.let { TitleLogic.libraryStatus(it) }

    val libraryAlert: Boolean get() = libraryStatus?.kind == LibraryKind.FAILED || (owned?.show?.failed ?: 0) > 0

    val pill: HeroPill
        get() =
            when {
                hidden && access.mayHide -> HeroPill.HIDDEN
                neverAdd -> HeroPill.NEVER_ADD
                else ->
                    when (ask?.state) {
                        "asked" -> HeroPill.ASKED
                        "approved" -> HeroPill.APPROVED
                        "turned_down" -> HeroPill.TURNED_DOWN
                        "available" -> HeroPill.AVAILABLE
                        else -> HeroPill.NONE
                    }
            }

    val askIsMine: Boolean get() = ask?.mine == true

    val canAdd: Boolean get() = access.manage && access.kindOn && owned == null && !neverAdd && ready

    val canCheckReleases: Boolean get() = canAdd && !isShow

    val askOffer: AskOffer?
        get() {
            if (!access.mayAsk || neverAdd || !ready) return null
            val o = owned
            if (o == null) {
                return when {
                    ask == null -> AskOffer.ASK
                    !isShow && ask?.mine == true -> AskOffer.ASKED
                    else -> AskOffer.ASK_TOO
                }
            }
            return if (isShow && seasonsToAsk.isNotEmpty()) AskOffer.SEASONS else null
        }

    val seasonsToAsk: List<Int>
        get() {
            val o = owned ?: return emptyList()
            return TitleLogic.seasonsToAsk(details?.seasons.orEmpty().map { it.number }, o)
        }

    val fourKOffer: AskOffer?
        get() {
            val m = movie
            if (!access.mayAsk || !fourKOn || isShow || neverAdd || m == null || m.has4k) return null
            if (m.ask4k?.mine == true) return AskOffer.ASKED
            return if (owned != null || m.ask?.mine == true) AskOffer.ASK else null
        }

    val primaryRuns: Boolean get() =
        access.manage &&
            (libraryStatus?.kind == LibraryKind.FAILED || libraryStatus?.kind == LibraryKind.MISSING)

    val mayReport: Boolean get() = owned != null && ((owned.slots.any { it.file != null }) || (owned.show?.inLibrary ?: 0) > 0)

    val actionGroups: List<List<TitleAction>>
        get() {
            val o = owned
            val groups = mutableListOf<List<TitleAction>>()
            if (o != null && access.manage) {
                groups += listOf(TitleAction.SEARCH, TitleAction.CHOOSE_RELEASE)
                groups += listOf(TitleAction.RENAME, TitleAction.ADD_VERSION, TitleAction.SETTINGS)
                groups +=
                    listOf(
                        if (o.keepUpToDate) TitleAction.STOP_KEEPING else TitleAction.KEEP,
                        TitleAction.REFRESH,
                        TitleAction.HISTORY,
                        TitleAction.DELETE,
                    )
            }
            if (mayReport) groups += listOf(TitleAction.REPORT)
            if (access.mayHide && hasTmdb && details != null) groups += listOf(TitleAction.HIDE)
            return groups
        }

    val trailers: List<TitleVideo> get() = TitleLogic.trailerRow(details?.videos.orEmpty(), language)

    val trailer: TitleVideo? get() = trailers.firstOrNull()

    val directors: List<TitlePerson>
        get() =
            details
                ?.people
                .orEmpty()
                .filter { it.role == if (isShow) "creator" else "director" }
                .take(2)

    val starring: List<TitlePerson> get() =
        details
            ?.people
            .orEmpty()
            .filter { it.role == "actor" }
            .take(3)

    val cast: List<TitlePerson> get() = details?.people.orEmpty()

    val scores: List<TitleScore> get() = TitleLogic.cleanScores(extras.scores)

    val tmdbScore: Double get() = details?.takeIf { it.votes >= TitleLogic.MIN_TMDB_VOTES }?.score ?: 0.0

    val factScores: List<TitleScore> get() = TitleLogic.shownScores(scores, tmdbScore).first

    val factTmdb: Double get() = TitleLogic.shownScores(scores, tmdbScore).second ?: 0.0

    val awardBadges: List<String> get() = wiki.badges.take(2)

    val wins: Int get() = wiki.awards.count { it.won }

    val nominations: Int get() = wiki.awards.count { !it.won }

    val collectionName: String get() = details?.collection?.name.orEmpty()

    val collectionParts: List<TitleCard> get() =
        details
            ?.collection
            ?.parts
            .orEmpty()
            .filter { it.tmdbId != tmdb }

    val similar: List<TitleCard>
        get() {
            val inCollection =
                details
                    ?.collection
                    ?.parts
                    .orEmpty()
                    .map { it.tmdbId }
                    .toSet()
            return details
                ?.similar
                .orEmpty()
                .filter { it.tmdbId !in inCollection && it.tmdbId != tmdb }
                .take(18)
        }

    val moreFromItems: List<TitleCard> get() = moreFrom.items.takeIf { it.size >= 4 }.orEmpty()

    val leadPerson: TitlePerson? get() = details?.people.orEmpty().firstOrNull { it.role == "actor" && it.id.isNotEmpty() }

    val leadItems: List<TitleCard>
        get() {
            val seen = moreFromItems.map { it.tmdbId }.toSet() + tmdb
            return lead.filter { it.tmdbId !in seen }.takeIf { it.size >= 4 }.orEmpty()
        }

    val facts: List<TitleFact>
        get() {
            val d = details ?: return emptyList()
            val out = mutableListOf<TitleFact>()
            val dates = d.dates
            if (isShow) {
                owned
                    ?.show
                    ?.network
                    ?.takeIf { it.isNotBlank() }
                    ?.let { out += TitleFact(FactKind.NETWORK, it) }
                dates?.firstAir?.takeIf { it.isNotBlank() }?.let { first ->
                    val ended = d.status == "Ended" || d.status == "Canceled"
                    out += TitleFact(FactKind.AIRED, first.take(4), end = if (ended) dates.lastAir.take(4) else "")
                }
                if (seasonCount > 0) {
                    out +=
                        TitleFact(
                            FactKind.SEASONS,
                            number = seasonCount.toLong(),
                            episode = d.seasons.filter { it.number > 0 }.sumOf { it.episodeCount },
                        )
                }
            } else {
                d.studios.firstOrNull()?.let { out += TitleFact(FactKind.STUDIO, it) }
                val released = dates?.theaters.orEmpty()
                if (released.isNotBlank()) {
                    out += TitleFact(FactKind.RELEASED, released)
                } else if (d.year > 0) {
                    out += TitleFact(FactKind.RELEASED_YEAR, d.year.toString())
                }
                if (d.runtimeMinutes > 0) out += TitleFact(FactKind.RUNTIME, number = d.runtimeMinutes.toLong())
            }
            if (d.rating.isNotBlank()) out += TitleFact(FactKind.RATED, d.rating)
            if (isShow && d.status.isNotBlank()) out += TitleFact(FactKind.STATUS, d.status)
            if (d.originCountry.isNotEmpty()) out += TitleFact(FactKind.ORIGIN, d.originCountry.joinToString(","))
            if (d.originalLanguage.isNotBlank()) out += TitleFact(FactKind.LANGUAGE, d.originalLanguage)
            if (d.spokenLanguages.size > 1) out += TitleFact(FactKind.SPOKEN, d.spokenLanguages.joinToString(", "))
            if (d.originalTitle.isNotBlank()) out += TitleFact(FactKind.ORIGINAL_TITLE, d.originalTitle)
            val aka =
                owned
                    ?.otherNames
                    .orEmpty()
                    .filter { it != d.originalTitle && it != d.name }
                    .take(5)
            if (aka.isNotEmpty()) out += TitleFact(FactKind.ALSO_KNOWN, aka.joinToString(" · "))
            if (!isShow) {
                dates?.digital?.takeIf { it.isNotBlank() }?.let { out += TitleFact(FactKind.DIGITAL, it) }
                dates?.physical?.takeIf { it.isNotBlank() }?.let { out += TitleFact(FactKind.DISC, it) }
            }
            d.nextEpisode?.takeIf { it.season > 0 }?.let {
                out += TitleFact(FactKind.NEXT_EPISODE, it.airDate, season = it.season, episode = it.episode)
            }
            if (d.budget > 0) out += TitleFact(FactKind.BUDGET, number = d.budget)
            if (d.revenue > 0) out += TitleFact(FactKind.REVENUE, number = d.revenue)
            return out
        }

    val links: List<TitleLink>
        get() {
            if (!hasTmdb) return emptyList()
            val out = mutableListOf<TitleLink>()
            val imdb = movie?.ids?.get("imdb") ?: show?.ids?.get("imdb") ?: owned?.ids?.get("imdb")
            if (!imdb.isNullOrBlank()) out += TitleLink("IMDb", "https://www.imdb.com/title/$imdb/")
            out += TitleLink("TMDB", "https://www.themoviedb.org/${if (isShow) "tv" else "movie"}/$tmdb")
            out += TitleLink("Trakt", "https://trakt.tv/search/tmdb/$tmdb?id_type=${if (isShow) "show" else "movie"}")
            if (!isShow) out += TitleLink("Letterboxd", "https://letterboxd.com/tmdb/$tmdb/")
            return out
        }

    val statusSub: StatusSub?
        get() {
            val o = owned ?: return null
            val s = libraryStatus ?: return null
            val handsOff = access.handsOff || TitleLogic.standInFolder(o.folderPath)
            val leftThere = o.show == null && o.slots.none { it.file != null } && o.slots.any { TitleLogic.leftInDownloader(it) }
            return when {
                leftThere -> StatusSub.LEFT_IN_DOWNLOADER
                s.kind == LibraryKind.DOWNLOADING && handsOff -> StatusSub.DOWNLOADING_HANDS_OFF
                s.kind == LibraryKind.HAVE && o.show != null -> StatusSub.FULL_SEASONS
                s.kind == LibraryKind.HAVE -> StatusSub.HAVE_MOVIE
                s.kind == LibraryKind.DOWNLOADING -> StatusSub.ON_ITS_WAY
                s.kind == LibraryKind.FAILED -> StatusSub.LAST_TRY_FAILED
                o.keepUpToDate -> StatusSub.WAITS_FOR_COPY
                else -> StatusSub.NOT_KEPT
            }
        }

    val fullSeasons: Int get() = owned?.show?.let { TitleLogic.fullSeasons(it) } ?: 0

    val ownBadges: List<String> get() = TitleLogic.qualityBadges(owned?.slots.orEmpty().mapNotNull { it.file })

    val mainFile: ch.orellbuehler.acolyte.api.model.OwnedFile? get() = owned?.slots?.firstNotNullOfOrNull { it.file }

    val statusPercent: Int?
        get() {
            val o = owned ?: return null
            if (o.show != null) return null
            val progress = TitleLogic.activeSlot(o)?.download?.progress ?: return null
            return (progress.coerceIn(0.0, 1.0) * 100).toInt()
        }

    val statusButton: StatusButton
        get() =
            when {
                !access.manage -> StatusButton.NONE
                libraryStatus?.kind == LibraryKind.FAILED -> StatusButton.TRY_AGAIN
                libraryStatus?.kind == LibraryKind.MISSING -> StatusButton.SEARCH_NOW
                else -> StatusButton.NONE
            }

    val folderName: String
        get() {
            val path = owned?.folderPath.orEmpty()
            if (TitleLogic.standInFolder(path)) return ""
            return path.split('/', '\\').lastOrNull { it.isNotEmpty() }.orEmpty()
        }

    val activeDownloads: List<OwnedSlot> get() =
        if (access.manage) {
            owned?.slots.orEmpty().filter {
                it.download?.active == true
            }
        } else {
            emptyList()
        }

    val slotCards: List<SlotCard>
        get() {
            val o = owned ?: return emptyList()
            return o.slots.map { slot ->
                val version = o.versions.firstOrNull { it.id == slot.versionId }
                val file = slot.file
                val badges = mutableListOf<String>()
                if (file != null) {
                    badges += TitleLogic.qualityBadges(listOf(file))
                    if (file.edition.isNotBlank()) badges += file.edition
                }
                SlotCard(
                    slot = slot,
                    label = slot.version,
                    main = version?.main ?: true,
                    badges = badges,
                    versionId = slot.versionId,
                    removable = version?.main == false,
                )
            }
        }

    val movieFileHeading: Boolean get() = (owned?.versions?.size ?: 0) <= 1

    val history: List<HistoryRow>
        get() {
            val seen = mutableSetOf<Long>()
            return events
                .filter { it.kind !in routineKinds }
                .filter { event ->
                    val id = event.downloadId ?: 0
                    id <= 0 || seen.add(id)
                }.map { HistoryRow(it, TitleLogic.eventTone(it.kind)) }
        }

    val heldDaysLeft: String get() = held.mapNotNull { it.expiresAt.ifBlank { null } }.minOrNull().orEmpty()

    val kindFolders: List<LibraryFolder> get() = TitleLogic.foldersFor(folders, kind)

    val kindRules: List<PickRule> get() = TitleLogic.rulesFor(rules, kind)

    val qualityPresets: List<PickRule>
        get() {
            val order = listOf("Lite", "Standard", "Best")
            return kindRules.filter { it.preset && it.name in order }.sortedBy { order.indexOf(it.name) }
        }

    val seasonPick: SeasonPick?
        get() {
            val show = owned?.show ?: return null
            return TitleLogic.seasonPick(show.seasons, show.newSeasons)
        }

    val showOffHint: Boolean get() = owned?.let { o -> o.show?.let { TitleLogic.showOffHint(o.keepUpToDate, it.seasons) } } ?: false

    val getAllMissing: Boolean
        get() = access.manage && (owned?.let { o -> o.show?.let { TitleLogic.getAllMissingShown(o.keepUpToDate, it.seasons) } } ?: false)

    val gateNote: Boolean get() = owned?.let { (!it.gateOpen || !it.keepUpToDate) && it.gateReason.isNotBlank() } ?: false

    internal companion object {
        val routineKinds =
            setOf(
                "search_done",
                "feed_checked",
                "title_refreshed",
                "albums_changed",
                "album_changed",
                "episodes_changed",
                "details_source_changed",
                "season_changed",
                "title_edited",
                "library_scanned",
                "library_rescanned",
                "folder_scanned",
                "file_back",
                "media_server_told",
            )
    }
}
