package ch.orellbuehler.acolyte.feature.home

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.AuthEndpoints
import ch.orellbuehler.acolyte.api.DiscoverEndpoints
import ch.orellbuehler.acolyte.api.DownloadsEndpoints
import ch.orellbuehler.acolyte.api.HealthEndpoints
import ch.orellbuehler.acolyte.api.HomeEndpoints
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.Part
import ch.orellbuehler.acolyte.api.RequestsEndpoints
import ch.orellbuehler.acolyte.api.model.Ask
import ch.orellbuehler.acolyte.api.model.AskLimit
import ch.orellbuehler.acolyte.api.model.HealthCheck
import ch.orellbuehler.acolyte.api.model.HomeAnswer
import ch.orellbuehler.acolyte.api.model.HomeCard
import ch.orellbuehler.acolyte.api.model.HomeDecision
import ch.orellbuehler.acolyte.api.model.HomeDownload
import ch.orellbuehler.acolyte.api.model.HomeHealth
import ch.orellbuehler.acolyte.api.model.LibraryFolder
import ch.orellbuehler.acolyte.api.model.Me
import ch.orellbuehler.acolyte.api.model.PopularAnswer
import ch.orellbuehler.acolyte.api.model.toHome
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.discover.CardFresh
import ch.orellbuehler.acolyte.feature.discover.CardKind
import ch.orellbuehler.acolyte.feature.discover.CardRow
import ch.orellbuehler.acolyte.feature.discover.CardRules
import ch.orellbuehler.acolyte.feature.discover.DiscoverCard
import ch.orellbuehler.acolyte.feature.discover.FreshKind
import ch.orellbuehler.acolyte.feature.discover.OwnedIndex
import ch.orellbuehler.acolyte.feature.discover.OwnedLibrary
import ch.orellbuehler.acolyte.feature.discover.cardRow
import ch.orellbuehler.acolyte.feature.discover.epochDay
import ch.orellbuehler.acolyte.live.LiveEvent
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.Role
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

enum class HomeBoard {
    HOME,
    SYSTEM,
}

enum class HomeTileKind {
    SETUP,
    ATTENTION,
    ASKS,
    LIBRARY,
    DISK,
    WEEK,
    DOWNLOADS,
    NEW,
    EXPLORE,
    POPULAR,
    CALENDAR,
    SEARCH,
}

data class HomeTile(
    val kind: HomeTileKind,
    val target: Section?,
)

enum class PictureLabel {
    NEW_SEASON,
    ARRIVED,
    COMING_SOON,
    FOLLOWING,
}

data class HomePicture(
    val card: DiscoverCard,
    val label: PictureLabel,
    val arrived: ArrivedTitle,
)

enum class SetupStepKind {
    FOLDER,
    SOURCE,
    DOWNLOADER,
    TITLE,
}

data class SetupStep(
    val kind: SetupStepKind,
    val done: Boolean,
    val target: Section,
)

data class AsksTile(
    val approver: Boolean,
    val waiting: Int,
    val posters: List<String> = emptyList(),
    val open: Int,
    val declined: Int,
    val failed: Boolean,
)

data class HomeLibraryKind(
    val kind: CardKind,
    val titles: Int,
    val parts: Int,
    val items: Int,
)

data class DiskTile(
    val size: Long,
    val free: FreeSpace?,
    val tone: Tone,
    val failed: Boolean,
)

data class DownloadsTile(
    val fetching: Int,
    val downloading: Int,
    val importing: Int,
    val waiting: Int,
    val paused: Int,
)

data class WeekTile(
    val count: Int,
    val posters: List<String>,
    val borrowed: Boolean,
)

data class AttentionTile(
    val rows: List<AttentionRow>,
    val worst: Tone,
    val unchecked: Boolean,
    val more: Int,
)

data class WelcomeLimit(
    val kind: CardKind,
    val noLimit: Boolean,
    val perWeek: Int,
    val left: Int,
)

data class JoinedWelcome(
    val name: String,
    val canAsk: Boolean,
    val kinds: List<CardKind>,
    val noLimit: Boolean,
    val limits: List<WelcomeLimit>,
)

data class SystemDownload(
    val id: Long,
    val name: String,
    val stage: DownloadStage,
    val progress: Double,
    val speed: Double,
    val finishedAt: String,
)

data class SystemBoard(
    val speed: Double = 0.0,
    val waiting: Int = 0,
    val downloading: Int = 0,
    val importing: Int = 0,
    val running: List<SystemDownload> = emptyList(),
    val finished: List<SystemDownload> = emptyList(),
    val paused: Int = 0,
    val notSent: Int = 0,
    val downloadsFailed: Boolean = false,
    val attention: List<AttentionRow> = emptyList(),
    val passed: Int = 0,
    val unchecked: Boolean = false,
    val sources: PartCount? = null,
    val downloaders: PartCount? = null,
    val sourcesOff: Boolean = false,
    val uptimeSeconds: Long = 0,
    val version: String = "",
    val healthFailed: Boolean = false,
)

data class HomeState(
    val status: LoadStatus = LoadStatus(),
    val greeting: GreetingTime = GreetingTime.MORNING,
    val name: String = "",
    val news: HomeNews? = null,
    val kid: Boolean = false,
    val system: Boolean = false,
    val board: HomeBoard = HomeBoard.HOME,
    val admin: Boolean = false,
    val owner: Boolean = false,
    val finishSetup: Boolean = false,
    val joined: JoinedWelcome? = null,
    val welcome: Boolean = false,
    val steps: List<SetupStep> = emptyList(),
    val pictures: List<HomePicture> = emptyList(),
    val tiles: List<HomeTile> = emptyList(),
    val attention: AttentionTile? = null,
    val asks: AsksTile? = null,
    val library: List<HomeLibraryKind> = emptyList(),
    val disk: DiskTile? = null,
    val downloads: DownloadsTile? = null,
    val week: WeekTile? = null,
    val newArrivals: Int = 0,
    val newSeasons: Int = 0,
    val popularArt: String? = null,
    val calendarArt: String? = null,
    val popularBuilding: Boolean = false,
    val arrived: CardRow? = null,
    val arrivedTitle: ArrivedTitle = ArrivedTitle.JUST_ARRIVED,
    val arrivedRecent: Boolean = false,
    val comingUp: List<ComingUp> = emptyList(),
    val borrowed: List<CardRow> = emptyList(),
    val systemBoard: SystemBoard? = null,
    val settled: Boolean = false,
) {
    val libraryTotal: Int get() = library.sumOf { it.titles }

    val nextStep: SetupStep? get() = steps.firstOrNull { !it.done }

    val stepsDone: Int get() = steps.count { it.done }
}

internal fun homeStrip(card: HomeCard): CardFresh? {
    val line = card.line
    if (line.isEmpty() || line == "Show" || line == "Music") return null
    return when (card.kind) {
        "show" -> CardFresh(if (line.startsWith("Season") && !line.contains("·")) FreshKind.NEW_SEASON else FreshKind.NEW_EPISODE, line)
        "artist" -> CardFresh(FreshKind.RECENTLY_ADDED, "${card.name} · $line")
        else -> null
    }
}

internal fun HomeCard.card(
    owned: OwnedIndex,
    row: String,
): DiscoverCard {
    val item = owned.byTitleId(titleId)
    return DiscoverCard(
        key = "$row-$key",
        kind = CardKind.of(kind),
        tmdb = item?.tmdb.orEmpty(),
        titleId = titleId,
        name = name,
        year = year,
        subtitle = line,
        poster = poster,
        backdrop = backdrop,
        logo = if (kind == "artist" || backdrop == null) null else logo,
        fresh = homeStrip(this),
        rowType = row,
    )
}

class HomePresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    private val owned: OwnedLibrary,
    private val offsetSeconds: Int,
    private val clock: () -> Long = ::systemMillis,
) {
    private val mutableState = MutableStateFlow(HomeState())
    private val lock = Mutex()
    private var home: HomeAnswer? = null
    private var me: Me? = null
    private var asks: List<Ask>? = null
    private var limit: AskLimit? = null
    private var folders: List<LibraryFolder>? = null
    private var downloads: List<HomeDownload>? = null
    private var health: HomeHealth? = null
    private var checks: List<HealthCheck>? = null
    private var decisions: List<HomeDecision>? = null
    private var popular: PopularAnswer? = null
    private var borrowedRows: List<CardRow> = emptyList()
    private val seenDownloads = mutableMapOf<Long, String>()

    val state: StateFlow<HomeState> = mutableState.asStateFlow()

    private fun now() = LocalTime(clock(), offsetSeconds)

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    fun show(board: HomeBoard) {
        mutableState.update { it.copy(board = board) }
    }

    suspend fun refresh() {
        lock.withLock {
            mutableState.update { it.copy(status = it.status.started()) }
            val account = account()
            val manager = account?.can(Need.MANAGE_LIBRARY) == true
            val canAsk = account != null && (account.can(Need.ASK) || account.can(Need.APPROVE)) && account.isOn(Part.WISHLIST)
            val index = owned.get()
            val first =
                coroutineScope {
                    val homeCall = async { api.result(HomeEndpoints.home) }
                    val meCall = async { api.result(AuthEndpoints.me).valueOrNull() }
                    homeCall.await() to meCall.await()
                }
            me = first.second ?: me
            when (val answer = first.first) {
                is ApiResult.Ok -> home = answer.value
                is ApiResult.Failed -> {
                    mutableState.update { it.copy(status = it.status.failed(answer.error)) }
                    if (home == null) {
                        publish(index)
                        return
                    }
                }
            }
            coroutineScope {
                val asksCall = async { if (canAsk) api.result(RequestsEndpoints.asks) else null }
                val asker = account?.allows(Need.ASK, Part.WISHLIST) == true && !manager
                val limitCall = async { if (asker) api.result(RequestsEndpoints.limit) else null }
                val foldersCall = async { if (manager) api.result(LibraryEndpoints.folders) else null }
                val downloadsCall = async { if (manager) api.result(DownloadsEndpoints.view, "limit" to DOWNLOAD_LIMIT) else null }
                val healthCall = async { if (manager) api.result(HomeEndpoints.health) else null }
                val checksCall = async { if (manager) api.result(HealthEndpoints.checks) else null }
                val decisionsCall = async { if (manager) api.result(DownloadsEndpoints.decisions) else null }
                val popularCall = async { api.result(DiscoverEndpoints.popular, "type" to "all", "page" to 0).valueOrNull() }
                val borrowCall =
                    async {
                        if (home?.rows?.contains("welcome") == true) {
                            api
                                .result(
                                    DiscoverEndpoints.head,
                                    "type" to "all",
                                    "tz" to null,
                                    "services" to "",
                                    "region" to null,
                                ).valueOrNull()
                        } else {
                            null
                        }
                    }
                asks = asksCall.await()?.let { (it as? ApiResult.Ok)?.value?.asks }
                limit = limitCall.await()?.valueOrNull()
                folders = foldersCall.await()?.valueOrNull()?.libraryFolders
                downloads =
                    downloadsCall
                        .await()
                        ?.valueOrNull()
                        ?.downloads
                        ?.map { it.toHome() }
                health = healthCall.await()?.valueOrNull()
                checks = checksCall.await()?.valueOrNull()?.checks
                decisions =
                    decisionsCall
                        .await()
                        ?.valueOrNull()
                        ?.items
                        ?.map { it.toHome() }
                popular = popularCall.await() ?: popular
                val head = borrowCall.await()
                val rules = CardRules()
                borrowedRows =
                    head
                        ?.head
                        .orEmpty()
                        .filter { row ->
                            row.items.count { (it.kind == "movie" || it.kind == "show") && (it.backdrop ?: it.poster) != null } >=
                                5
                        }.take(2)
                        .map { it.cardRow(rules, index).copy(seeAll = true) }
            }
            if (first.first is ApiResult.Ok) mutableState.update { it.copy(status = LoadStatus.ready) }
            publish(index)
        }
    }

    suspend fun dismissWelcome() {
        api.result(LibraryEndpoints.views, JsonObject(mapOf(WELCOME_SEEN to JsonPrimitive("1"))))
        me = me?.copy(views = JsonObject(me?.views.orEmpty() + (WELCOME_SEEN to JsonPrimitive("1"))))
        publish(owned.index.value)
    }

    suspend fun watch() {
        coroutineScope {
            var pending: Job? = null
            launch {
                api.live.collect { event ->
                    if (event.topic !in TOPICS) return@collect
                    if (event.topic == "download" && !downloadMoved(event)) return@collect
                    pending?.cancel()
                    pending =
                        launch {
                            delay(LIVE_DELAY)
                            refresh()
                        }
                }
            }
            launch {
                while (true) {
                    val answer = home
                    val downloading = answer?.askProgress?.isNotEmpty() == true
                    val waitingCopy = asks.orEmpty().any { it.mine && it.state == "approved" }
                    delay(if (downloading) FAST_POLL else SLOW_POLL)
                    if (downloading || waitingCopy || account()?.can(Need.MANAGE_LIBRARY) != true) refresh()
                }
            }
        }
    }

    private fun downloadMoved(event: LiveEvent): Boolean {
        val data = event.data as? JsonObject ?: return true
        val id = ((data["ID"] ?: data["id"]) as? JsonPrimitive)?.contentOrNull?.toLongOrNull() ?: return true
        val state = ((data["State"] ?: data["state"]) as? JsonPrimitive)?.contentOrNull ?: return true
        if (seenDownloads[id] == state) return false
        seenDownloads[id] = state
        return true
    }

    private fun publish(index: OwnedIndex) {
        val account = account()
        val answer = home
        val now = now()
        val today = now.isoDay()
        val manager = account?.can(Need.MANAGE_LIBRARY) == true
        val approver = account?.can(Need.APPROVE) == true && account.isKid == false
        val admin = account?.can(Need.SETTINGS) == true
        val kid = account?.isKid == true || answer?.kid == true
        val owner = answer?.owner == true
        val canAsk = account != null && (account.can(Need.ASK) || approver) && account.isOn(Part.WISHLIST)
        val rows = answer?.rows.orEmpty()
        val welcome = "welcome" in rows
        val name = greetingName(account?.displayName.orEmpty(), account?.userName.orEmpty())
        val sourcesOff = account?.isOn(Part.SOURCES) == false

        val failingChecks = { kind: String -> checks.orEmpty().filter { it.kind == kind && shownState(it) != "ok" } }
        val sources = health?.let { partCount(it.sources.orEmpty(), failingChecks("source").map { c -> c.subject }) }
        val downloaders = health?.let { partCount(it.downloaders.orEmpty(), failingChecks("downloader").map { c -> c.subject }) }
        val steps =
            buildList {
                add(SetupStep(SetupStepKind.FOLDER, folders.orEmpty().isNotEmpty(), Section.SETTINGS))
                if (!sourcesOff) add(SetupStep(SetupStepKind.SOURCE, (sources?.total ?: 0) > 0, Section.SETTINGS))
                add(SetupStep(SetupStepKind.DOWNLOADER, (downloaders?.total ?: 0) > 0, Section.SETTINGS))
                add(SetupStep(SetupStepKind.TITLE, false, Section.SEARCH))
            }

        val seen = mutableSetOf<Long>()
        val pictures = mutableListOf<HomePicture>()
        val lists =
            listOf(
                answer?.newSeasons.orEmpty() to PictureLabel.NEW_SEASON,
                answer?.arrived.orEmpty() to PictureLabel.ARRIVED,
                answer?.soon.orEmpty() to PictureLabel.COMING_SOON,
                answer?.following.orEmpty() to PictureLabel.FOLLOWING,
            )
        var round = 0
        while (pictures.size < PICTURES && lists.any { round < it.first.size }) {
            for ((list, label) in lists) {
                val card = list.getOrNull(round) ?: continue
                if (pictures.size >= PICTURES || (card.backdrop ?: card.poster) == null || !seen.add(card.titleId)) continue
                pictures += HomePicture(card.card(index, "tile"), label, arrivedTitle(epochMillisOf(card.at), now.epochMillis))
            }
            round++
        }
        val onTiles = pictures.map { it.card.titleId }.toSet()

        val library =
            listOf("movie" to CardKind.MOVIE, "show" to CardKind.SHOW, "artist" to CardKind.ARTIST).mapNotNull { (key, kind) ->
                answer?.library?.get(key)?.let { HomeLibraryKind(kind, it.titles, it.parts, it.items) }
            }
        val diskWarn = checks.orEmpty().filter { it.kind == "disk_space" }.map(::shownState)
        val diskTone =
            when {
                "error" in diskWarn -> Tone.FAIL
                "warning" in diskWarn -> Tone.WAIT
                else -> Tone.OK
            }
        val disk =
            if (owner && library.isNotEmpty()) {
                DiskTile(
                    size =
                        answer
                            ?.library
                            ?.values
                            .orEmpty()
                            .sumOf { it.size },
                    free = folders?.let { list -> freeSpace(list.map { it.freeSpace }, diskTone == Tone.FAIL) },
                    tone = diskTone,
                    failed = manager && folders == null,
                )
            } else {
                null
            }
        val list = downloads.orEmpty()
        val counted = list.filter { homeCounts(it) }
        val fetching = downloadingNow(list)
        val stages = counted.map(::stageOf)
        val downloadsTile =
            if (manager && fetching.isNotEmpty()) {
                DownloadsTile(
                    fetching = fetching.size,
                    downloading = fetching.count { stageOf(it) == DownloadStage.DOWNLOADING || stageOf(it) == DownloadStage.STALLED },
                    importing = fetching.count { stageOf(it) == DownloadStage.IMPORTING || stageOf(it) == DownloadStage.CHECKING },
                    waiting = fetching.count { stageOf(it) == DownloadStage.QUEUED },
                    paused = stages.count { it == DownloadStage.PAUSED },
                )
            } else {
                null
            }

        val failed = counted.filter { stageOf(it) == DownloadStage.FAILED }
        val attentionRows =
            if (owner) {
                attention(
                    failed,
                    if (manager) decisions.orEmpty() else emptyList(),
                    checks.orEmpty(),
                    admin,
                )
            } else {
                emptyList()
            }
        val unchecked = owner && (downloads == null || checks == null || health == null)
        val worst =
            when {
                attentionRows.any { it.tone == Tone.FAIL } -> Tone.FAIL
                attentionRows.isNotEmpty() || unchecked -> Tone.WAIT
                else -> Tone.OK
            }
        val attentionTile = if (owner) AttentionTile(attentionRows, worst, unchecked, (attentionRows.drop(1).sumOf { it.count })) else null

        val allAsks = asks
        val waiting = allAsks.orEmpty().filter { it.state == "asked" }
        val mine = allAsks.orEmpty().filter { it.mine && it.state != "available" }
        val asksTile =
            if (canAsk) {
                AsksTile(
                    approver = approver,
                    waiting = waiting.size,
                    posters = waiting.mapNotNull { it.poster }.take(3),
                    open = mine.count { it.state != "turned_down" },
                    declined = mine.count { it.state == "turned_down" },
                    failed = allAsks == null,
                )
            } else {
                null
            }

        val weekCards =
            answer?.soon.orEmpty().filter { card ->
                val day = epochDay(card.at.take(10)) ?: return@filter false
                day in now.day..(now.day + 6)
            }
        val borrowedWeek =
            popular?.rail.orEmpty().filter { day ->
                val d = epochDay(day.day) ?: return@filter false
                d in now.day..(now.day + 6)
            }
        val week =
            if (manager) {
                if (weekCards.isNotEmpty()) {
                    WeekTile(weekCards.size, weekCards.mapNotNull { it.poster }.take(3), false)
                } else {
                    val items = borrowedWeek.flatMap { it.items }
                    WeekTile(items.size, items.mapNotNull { it.poster }.take(3), true)
                }
            } else {
                null
            }
        val recentArrived =
            answer?.arrived.orEmpty().count { c ->
                epochMillisOf(c.at)?.let {
                    now.epochMillis - it <
                        14 * LocalTime.DAY_MILLIS
                } ==
                    true
            }
        val recentNew = recentArrived + answer?.newSeasons.orEmpty().size

        val libraryTarget =
            library.maxByOrNull { it.titles }?.let { sectionOf(it.kind) } ?: Section.LIBRARY
        val tiles =
            buildList {
                if (owner) add(HomeTile(if (welcome) HomeTileKind.SETUP else HomeTileKind.ATTENTION, null))
                if (canAsk) add(HomeTile(HomeTileKind.ASKS, Section.REQUESTS))
                if (library.isNotEmpty() && !(welcome && owner)) add(HomeTile(HomeTileKind.LIBRARY, libraryTarget))
                if (disk != null) add(HomeTile(HomeTileKind.DISK, if (admin) Section.SETTINGS else Section.HEALTH))
                if (manager) add(HomeTile(HomeTileKind.WEEK, Section.CALENDAR))
                if (downloadsTile != null) add(HomeTile(HomeTileKind.DOWNLOADS, Section.DOWNLOADS))
                if (recentNew > 0) {
                    val newest = (answer?.arrived.orEmpty() + answer?.newSeasons.orEmpty()).firstOrNull()
                    add(HomeTile(HomeTileKind.NEW, newest?.let { sectionOf(CardKind.of(it.kind)) } ?: Section.LIBRARY))
                }
                add(HomeTile(HomeTileKind.EXPLORE, Section.EXPLORE))
                add(HomeTile(HomeTileKind.POPULAR, Section.POPULAR))
                if (manager) add(HomeTile(HomeTileKind.CALENDAR, Section.CALENDAR))
                add(HomeTile(HomeTileKind.SEARCH, Section.SEARCH))
            }.map { tile ->
                val target =
                    when (tile.kind) {
                        HomeTileKind.SETUP -> steps.firstOrNull { !it.done }?.target ?: Section.SEARCH
                        HomeTileKind.ATTENTION ->
                            when (attentionRows.firstOrNull()?.target) {
                                AttentionTarget.SETTINGS -> Section.SETTINGS
                                AttentionTarget.DOWNLOADS -> Section.DOWNLOADS
                                else -> Section.HEALTH
                            }
                        else -> tile.target
                    }
                tile.copy(target = target?.takeIf { account?.shows(it) == true })
            }

        val arrivedCards = answer?.arrived.orEmpty().filter { it.titleId !in onTiles }
        val newest = arrivedCards.mapNotNull { epochMillisOf(it.at) }.maxOrNull()
        val arrivedRow =
            if (arrivedCards.isNotEmpty()) {
                CardRow("arrived", "arrived", "", cards = arrivedCards.map { it.card(index, "arrived") }, seeAll = true)
            } else {
                null
            }

        val coming = mutableListOf<ComingUp>()
        val upSeen = onTiles.toMutableSet()

        fun addUp(
            card: HomeCard,
            max: Int,
        ) {
            if (coming.size >= max || !upSeen.add(card.titleId)) return
            val (sub, whenKind, text) = upLine(card, today)
            coming +=
                ComingUp(
                    key = card.key,
                    titleId = card.titleId,
                    tmdb = index.byTitleId(card.titleId)?.tmdb.orEmpty(),
                    kind = card.kind,
                    name = card.name,
                    image = card.backdrop ?: card.poster,
                    sub = sub,
                    whenKind = whenKind,
                    date = if (whenKind == UpWhen.TEXT) "" else text,
                    text = if (whenKind == UpWhen.TEXT) text else "",
                )
        }
        answer
            ?.newSeasons
            .orEmpty()
            .filter { it.line.contains("out now", ignoreCase = true) }
            .forEach { addUp(it, 2) }
        answer?.soon.orEmpty().forEach { addUp(it, COMING_MAX) }
        answer?.newSeasons.orEmpty().forEach { addUp(it, COMING_MAX) }

        val views = me?.views.orEmpty()
        val seenWelcome = (views[WELCOME_SEEN] as? JsonPrimitive)?.contentOrNull == "1"
        val joined =
            if (account != null && account.role != Role.OWNER && me?.setupOpen != true && !seenWelcome) {
                val askOn = account.allows(Need.ASK, Part.WISHLIST)
                val kinds =
                    listOf(Part.MOVIES to CardKind.MOVIE, Part.SHOWS to CardKind.SHOW, Part.MUSIC to CardKind.ARTIST)
                        .filter { (part, kind) -> account.isOn(part) && !(kind == CardKind.ARTIST && account.isKid) }
                        .map { it.second }
                val limits =
                    limit
                        ?.let { l ->
                            kinds.map { kind ->
                                val k =
                                    when (kind) {
                                        CardKind.MOVIE -> l.movies
                                        CardKind.SHOW -> l.shows
                                        CardKind.ARTIST -> l.music
                                    }
                                WelcomeLimit(kind, k.noLimit, k.perWeek, k.left)
                            }
                        }.orEmpty()
                JoinedWelcome(name, askOn, kinds, limit?.noLimit ?: false, limits)
            } else {
                null
            }

        val systemBoard =
            if (manager && !kid) {
                val running = fetching.take(3).map { it.system() }
                val finishedRows =
                    if (fetching.isEmpty()) {
                        counted
                            .filter { stageOf(it) == DownloadStage.DONE || stageOf(it) == DownloadStage.SEEDING }
                            .sortedByDescending { it.finishedAt }
                            .take(3)
                            .map { it.system() }
                    } else {
                        emptyList()
                    }
                SystemBoard(
                    speed = fetching.sumOf { it.speed },
                    waiting = fetching.count { stageOf(it) == DownloadStage.QUEUED },
                    downloading = fetching.count { stageOf(it) == DownloadStage.DOWNLOADING || stageOf(it) == DownloadStage.STALLED },
                    importing = fetching.count { stageOf(it) == DownloadStage.IMPORTING || stageOf(it) == DownloadStage.CHECKING },
                    running = running,
                    finished = finishedRows,
                    paused = stages.count { it == DownloadStage.PAUSED },
                    notSent = notSentNow(list).size,
                    downloadsFailed = downloads == null,
                    attention = attentionRows,
                    passed = checks.orEmpty().count { shownState(it) == "ok" },
                    unchecked = unchecked,
                    sources = sources,
                    downloaders = downloaders,
                    sourcesOff = sourcesOff,
                    uptimeSeconds = health?.uptimeSeconds ?: 0,
                    version = health?.version.orEmpty(),
                    healthFailed = health == null,
                )
            } else {
                null
            }

        mutableState.update {
            it.copy(
                greeting = greetingTime(now.hour),
                name = name,
                news =
                    homeNews(
                        answer,
                        asks.orEmpty(),
                        list,
                        approver,
                        manager,
                        kid,
                        now,
                        account?.userName.orEmpty().lowercase(),
                    ),
                kid = kid,
                system = manager && !kid,
                board = if (manager && !kid) it.board else HomeBoard.HOME,
                admin = admin,
                owner = owner,
                finishSetup = me?.setupSkipped == true && account?.role == Role.OWNER,
                joined = joined,
                welcome = welcome,
                steps = if (owner) steps else emptyList(),
                pictures = pictures,
                tiles = tiles,
                attention = attentionTile,
                asks = asksTile,
                library = library,
                disk = disk,
                downloads = downloadsTile,
                week = week,
                newArrivals = recentArrived,
                newSeasons = answer?.newSeasons.orEmpty().size,
                popularArt = popular?.hero?.firstOrNull { h -> h.backdrop != null }?.backdrop,
                calendarArt =
                    answer
                        ?.soon
                        .orEmpty()
                        .firstOrNull { c -> c.backdrop != null }
                        ?.backdrop,
                popularBuilding = popular?.building == true,
                arrived = arrivedRow,
                arrivedTitle = arrivedTitle(newest, now.epochMillis),
                arrivedRecent = newest != null && now.epochMillis - newest < 14 * LocalTime.DAY_MILLIS,
                comingUp = coming,
                borrowed = if (welcome) borrowedRows else emptyList(),
                systemBoard = systemBoard,
                settled = true,
            )
        }
    }

    private fun HomeDownload.system() = SystemDownload(id, title.ifEmpty { name }, stageOf(this), progress, speed, finishedAt)

    private fun sectionOf(kind: CardKind): Section =
        when (kind) {
            CardKind.MOVIE -> Section.MOVIES
            CardKind.SHOW -> Section.SHOWS
            CardKind.ARTIST -> Section.MUSIC
        }

    private companion object {
        const val WELCOME_SEEN = "welcome.seen"
        const val DOWNLOAD_LIMIT = 100
        const val PICTURES = 3
        const val COMING_MAX = 6
        const val LIVE_DELAY = 250L
        const val FAST_POLL = 15_000L
        const val SLOW_POLL = 60_000L
        val TOPICS = setOf("title", "import", "resync", "health", "ask", "ask_removed", "download")
    }
}
