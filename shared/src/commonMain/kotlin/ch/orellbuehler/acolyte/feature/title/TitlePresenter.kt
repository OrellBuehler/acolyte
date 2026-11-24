package ch.orellbuehler.acolyte.feature.title

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.TitleEndpoints
import ch.orellbuehler.acolyte.api.TitleSupportEndpoints
import ch.orellbuehler.acolyte.api.model.AdoptBody
import ch.orellbuehler.acolyte.api.model.FileChoice
import ch.orellbuehler.acolyte.api.model.HideBody
import ch.orellbuehler.acolyte.api.model.JobIds
import ch.orellbuehler.acolyte.api.model.KeepBody
import ch.orellbuehler.acolyte.api.model.LookupMovie
import ch.orellbuehler.acolyte.api.model.LookupShow
import ch.orellbuehler.acolyte.api.model.OwnedEpisode
import ch.orellbuehler.acolyte.api.model.OwnedTitle
import ch.orellbuehler.acolyte.api.model.RestoreRequest
import ch.orellbuehler.acolyte.api.model.SearchBody
import ch.orellbuehler.acolyte.api.model.TitleCard
import ch.orellbuehler.acolyte.api.model.TitleChange
import ch.orellbuehler.acolyte.api.model.TitleMove
import ch.orellbuehler.acolyte.api.model.TitleRelease
import ch.orellbuehler.acolyte.api.model.VersionAudioChange
import ch.orellbuehler.acolyte.api.model.VersionInput
import ch.orellbuehler.acolyte.api.onFailure
import ch.orellbuehler.acolyte.api.onSuccess
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.live.LiveEvent
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
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
import kotlinx.serialization.json.longOrNull

class TitlePresenter internal constructor(
    private val api: Api,
    kind: String,
    tmdb: String,
    libraryId: Long,
    openLibrary: Boolean,
    private val account: Account?,
    language: String,
) {
    private val mutableState =
        MutableStateFlow(
            TitleState(
                kind = normalKind(kind),
                tmdb = tmdb.trim(),
                libraryId = libraryId,
                access = TitleAccess.of(account, normalKind(kind)),
                language = language,
                tab = if (openLibrary) TitleTab.LIBRARY else TitleTab.OVERVIEW,
            ),
        )
    private val lock = Mutex()
    private val feedback = FeedbackCounter()

    val state: StateFlow<TitleState> = mutableState.asStateFlow()

    private val current: TitleState get() = mutableState.value

    suspend fun load() {
        if (current.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            mutableState.update { it.copy(status = it.status.started()) }
            if (current.tmdb.isEmpty() && current.libraryId > 0) {
                when (val result = api.result(LibraryEndpoints.titleVersions, "id" to current.libraryId)) {
                    is ApiResult.Ok -> adoptOwned(result.value)
                    is ApiResult.Failed -> {
                        val problem = if (result.error.code == ApiErrorCode.NOT_FOUND) TitleProblem.NOT_IN_LIBRARY else TitleProblem.NONE
                        mutableState.update { it.copy(status = it.status.failed(result.error), problem = problem) }
                        return
                    }
                }
            }
            if (current.tmdb.isEmpty()) {
                mutableState.update { it.copy(status = LoadStatus.ready) }
                coroutineScope { loadOwnedExtras(this@coroutineScope) }
                return
            }
            loadPage()
        }
    }

    private fun adoptOwned(owned: OwnedTitle) {
        val tmdb =
            owned.tmdbId.takeIf { id ->
                id.all { it.isDigit() } &&
                    id.isNotEmpty() &&
                    id != owned.ids["tvdb"] &&
                    id != owned.ids["tvmaze"]
            }
        mutableState.update {
            it.copy(
                owned = owned,
                ownedError = null,
                kind = if (owned.kind == "show") "show" else "movie",
                tmdb = tmdb.orEmpty(),
                access = TitleAccess.of(account, owned.kind),
            )
        }
    }

    private suspend fun loadPage() =
        coroutineScope {
            val s = current
            val details = async { api.result(TitleEndpoints.details, "kind" to s.kind, "tmdb_id" to s.tmdb) }
            val lookup =
                async {
                    if (s.isShow) {
                        api.result(LibraryEndpoints.lookupShows, "term" to "tmdb:${s.tmdb}").valueOrNull()?.shows?.firstOrNull {
                            it.ids["tmdb"] == s.tmdb
                        }
                    } else {
                        api.result(LibraryEndpoints.lookupMovies, "term" to "tmdb:${s.tmdb}").valueOrNull()?.movies?.firstOrNull {
                            it.ids["tmdb"] == s.tmdb
                        }
                    }
                }
            val detailsResult = details.await()
            val found = lookup.await()
            val movie = found as? LookupMovie
            val show = found as? LookupShow
            when (detailsResult) {
                is ApiResult.Ok ->
                    mutableState.update {
                        it.copy(
                            details = detailsResult.value,
                            movie = movie,
                            show = show,
                            problem = TitleProblem.NONE,
                        )
                    }
                is ApiResult.Failed -> {
                    val error = detailsResult.error
                    val problem =
                        when {
                            error.code == ApiErrorCode.FORBIDDEN -> TitleProblem.BLOCKED
                            error.code == ApiErrorCode.NOT_FOUND && found == null && current.owned == null -> TitleProblem.NOT_FOUND
                            else -> TitleProblem.NONE
                        }
                    val usable = found != null || current.owned != null
                    if (problem != TitleProblem.NONE || !usable) {
                        mutableState.update { it.copy(status = it.status.failed(error), problem = problem, movie = movie, show = show) }
                        return@coroutineScope
                    }
                    mutableState.update { it.copy(movie = movie, show = show) }
                }
            }
            val titleId =
                current.libraryId.takeIf { it > 0 } ?: current.details?.titleId?.takeIf { it > 0 } ?: movie?.titleId?.takeIf { it > 0 }
                    ?: show?.titleId?.takeIf { it > 0 }
            if (titleId != null && current.owned?.id != titleId) {
                api
                    .result(LibraryEndpoints.titleVersions, "id" to titleId)
                    .onSuccess { owned -> mutableState.update { it.copy(owned = owned, ownedError = null) } }
                    .onFailure { error -> mutableState.update { it.copy(ownedError = error) } }
            }
            if (current.owned == null && current.tab == TitleTab.LIBRARY) mutableState.update { it.copy(tab = TitleTab.OVERVIEW) }
            mutableState.update { it.copy(status = LoadStatus.ready) }
            launch { loadOverviewExtras() }
            loadOwnedExtras(this)
        }

    private suspend fun loadOverviewExtras() =
        coroutineScope {
            val s = current
            launch {
                api.result(TitleEndpoints.extras, "kind" to s.kind, "id" to s.tmdb).onSuccess { v ->
                    mutableState.update { it.copy(extras = v) }
                }
            }
            launch {
                api.result(TitleEndpoints.wiki, "kind" to s.kind, "id" to s.tmdb).onSuccess { v ->
                    mutableState.update { it.copy(wiki = v) }
                }
            }
            launch {
                api
                    .result(TitleEndpoints.moreFrom, "kind" to s.kind, "tmdb_id" to s.tmdb)
                    .onSuccess { v -> mutableState.update { it.copy(moreFrom = v) } }
            }
            launch {
                val lead = current.leadPerson ?: return@launch
                api
                    .result(TitleSupportEndpoints.person, "id" to lead.id, "role" to "actor", "page" to 1, "page_size" to 24)
                    .onSuccess { page ->
                        val movies =
                            page.knownFor.ifEmpty { page.movies }.mapNotNull { m ->
                                val id = m.ids["tmdb"] ?: return@mapNotNull null
                                TitleCard(
                                    tmdbId = id,
                                    kind = "movie",
                                    name = m.name,
                                    year = m.year,
                                    poster = m.poster,
                                    backdrop = m.backdrop,
                                    titleId = m.titleId,
                                    inLibrary = m.titleId > 0,
                                    ask = m.ask,
                                )
                            }
                        mutableState.update { it.copy(lead = movies) }
                    }
            }
            if (s.access.mayAsk && !s.isShow) {
                launch {
                    api.result(TitleSupportEndpoints.askLimit).onSuccess { v -> mutableState.update { it.copy(fourKOn = v.fourK.movie) } }
                }
            }
            if (s.access.followOn && s.owned == null) launch { readFollow() }
        }

    private fun loadOwnedExtras(scope: kotlinx.coroutines.CoroutineScope) {
        val owned = current.owned ?: return
        val access = current.access
        if (access.followOn) scope.launch { readFollow() }
        scope.launch { loadEvents(owned.id) }
        if (access.manage) {
            scope.launch {
                api
                    .result(TitleEndpoints.watchLinks, "title" to owned.id)
                    .onSuccess { v -> mutableState.update { it.copy(watchLinks = v.links) } }
            }
            scope.launch { loadHeld(owned.id) }
            scope.launch { loadRenameCount(owned.id) }
            scope.launch {
                api.result(TitleEndpoints.leftovers, "id" to owned.id).onSuccess { v ->
                    mutableState.update { it.copy(leftoverCount = v.leftovers.count { l -> !l.dismissed }) }
                }
            }
        }
        val kind = current.libraryStatus?.kind
        if (kind != LibraryKind.HAVE && kind != LibraryKind.DOWNLOADING) {
            scope.launch {
                api.result(TitleEndpoints.outcome, "id" to owned.id).onSuccess { v -> mutableState.update { it.copy(outcome = v) } }
            }
        }
        if (current.legacy) {
            scope.launch {
                api.result(TitleEndpoints.people, "id" to owned.id).onSuccess { v -> mutableState.update { it.copy(credits = v.people) } }
            }
        }
    }

    private suspend fun loadEvents(id: Long) {
        if (!current.access.manage) return
        api.result(TitleSupportEndpoints.events, "title" to id, "limit" to 30).onSuccess { v ->
            mutableState.update { it.copy(events = v.events) }
        }
    }

    private suspend fun loadHeld(id: Long) {
        api.result(LibraryEndpoints.holding, "title_id" to id).onSuccess { v ->
            mutableState.update { it.copy(held = v.files.filter { f -> f.path.isNotEmpty() }) }
        }
    }

    private suspend fun loadRenameCount(id: Long) {
        api.result(TitleEndpoints.renamePlan, "id" to id).onSuccess { plan ->
            mutableState.update { it.copy(renameCount = plan.steps.count { s -> s.changes }) }
        }
    }

    suspend fun reloadOwned() {
        val id = current.owned?.id ?: return
        coroutineScope {
            launch { loadEvents(id) }
            api
                .result(LibraryEndpoints.titleVersions, "id" to id)
                .onSuccess { owned -> mutableState.update { it.copy(owned = owned, ownedError = null) } }
                .onFailure { error ->
                    if (error.code == ApiErrorCode.NOT_FOUND) mutableState.update { it.copy(owned = null, tab = TitleTab.OVERVIEW) }
                }
            if (current.access.manage) {
                launch { loadHeld(id) }
                launch { loadRenameCount(id) }
            }
        }
    }

    suspend fun retryOwned() {
        val id = current.details?.titleId?.takeIf { it > 0 } ?: current.libraryId.takeIf { it > 0 } ?: return
        api
            .result(LibraryEndpoints.titleVersions, "id" to id)
            .onSuccess { owned -> mutableState.update { it.copy(owned = owned, ownedError = null) } }
            .onFailure { error -> mutableState.update { it.copy(ownedError = error) } }
    }

    fun selectTab(tab: TitleTab) {
        if (tab == TitleTab.LIBRARY && current.owned == null) return
        mutableState.update { it.copy(tab = tab) }
    }

    suspend fun loadChoices(force: Boolean = false) {
        val s = current
        if (!s.access.manage || (s.choicesLoaded && !force)) return
        coroutineScope {
            val folders = async { api.result(LibraryEndpoints.folders) }
            val rules = async { api.result(LibraryEndpoints.pickRules) }
            val groups =
                async {
                    val id = s.owned?.id
                    if (s.isShow && id != null) api.result(TitleEndpoints.episodeGroups, "id" to id).valueOrNull() else null
                }
            val f = folders.await()
            val r = rules.await()
            val error = (f as? ApiResult.Failed)?.error ?: (r as? ApiResult.Failed)?.error
            mutableState.update {
                it.copy(
                    folders = f.valueOrNull()?.libraryFolders ?: it.folders,
                    rules = r.valueOrNull()?.pickRules ?: it.rules,
                    episodeGroups = groups.await() ?: it.episodeGroups,
                    choicesLoaded = error == null,
                    choicesError = error,
                )
            }
        }
    }

    suspend fun searchNow() {
        val owned = current.owned ?: return
        busy("search") {
            api
                .result(LibraryEndpoints.search, SearchBody(), "id" to owned.id)
                .onSuccess { answer -> say(searchFeedback(answer, FeedbackKind.SEARCHING, owned)) }
                .onFailure { say(feedback.failed(it)) }
        }
    }

    private fun searchFeedback(
        answer: JobIds,
        kind: FeedbackKind,
        owned: OwnedTitle,
        count: Int = 0,
        text: String = owned.name,
    ): Feedback {
        val ids = answer.jobIds
        if (ids.isEmpty() && owned.show != null) {
            return feedback.next(if (owned.keepUpToDate) FeedbackKind.NOTHING_TO_SEARCH else FeedbackKind.TURN_SHOW_ON)
        }
        return feedback.next(kind, text, count)
    }

    suspend fun searchMissing() {
        val owned = current.owned ?: return
        busy("missing") {
            api
                .result(LibraryEndpoints.search, SearchBody(), "id" to owned.id)
                .onSuccess { answer -> say(searchFeedback(answer, FeedbackKind.SEARCHING_MISSING, owned)) }
                .onFailure { say(feedback.failed(it)) }
        }
    }

    suspend fun searchSeason(season: Int) {
        val owned = current.owned ?: return
        busy("season-$season") {
            api
                .result(LibraryEndpoints.search, SearchBody(season = season), "id" to owned.id)
                .onSuccess { answer -> say(searchFeedback(answer, FeedbackKind.SEARCHING_SEASON, owned, season)) }
                .onFailure { say(feedback.failed(it)) }
        }
    }

    suspend fun searchSlot(
        slotId: Long,
        code: String,
    ) {
        val owned = current.owned ?: return
        busy("slot-$slotId") {
            api
                .result(LibraryEndpoints.search, SearchBody(slotId = slotId), "id" to owned.id)
                .onSuccess { answer -> say(searchFeedback(answer, FeedbackKind.SEARCHING_EPISODE, owned, text = code)) }
                .onFailure { say(feedback.failed(it)) }
        }
    }

    suspend fun toggleKeep() {
        val owned = current.owned ?: return
        change("keep", TitleChange(keepUpToDate = !owned.keepUpToDate))
    }

    suspend fun setRules(rulesId: Long) = change("rules", TitleChange(pickRulesId = rulesId))

    suspend fun setPackWindow(window: Int) = change("packs", TitleChange(packWindow = window))

    suspend fun setGate(
        gate: String,
        delayDays: Int,
    ) = change("gate", TitleChange(releaseGate = gate, gateDelayDays = delayDays.coerceIn(0, 365)))

    suspend fun setOrder(order: String) {
        val grouped =
            current.owned
                ?.show
                ?.episodeGroup
                ?.id
                ?.isNotEmpty() == true
        change("order", if (grouped) TitleChange(episodeGroupId = "", episodeOrder = order) else TitleChange(episodeOrder = order))
    }

    suspend fun setEpisodeGroup(groupId: String) = change("order", TitleChange(episodeGroupId = groupId))

    private suspend fun change(
        key: String,
        body: TitleChange,
    ) {
        val owned = current.owned ?: return
        busy(key) {
            api
                .result(LibraryEndpoints.change, body, "id" to owned.id)
                .onSuccess { answer ->
                    mutableState.update { it.copy(owned = answer.title) }
                    if (body.episodeGroupId != null || body.episodeOrder != null) {
                        mutableState.update { it.copy(choicesLoaded = false) }
                        if (answer.renameSuggested) say(feedback.next(FeedbackKind.ORDER_CHANGED))
                    }
                }.onFailure { say(feedback.failed(it)) }
        }
        if (body.episodeGroupId != null) loadChoices(force = true)
    }

    suspend fun setSeasons(pick: SeasonPick) {
        val owned = current.owned ?: return
        val show = owned.show ?: return
        busy("seasons") {
            for ((number, keep) in TitleLogic.seasonKeepPlan(show.seasons, pick)) {
                val result = api.result(TitleEndpoints.keepSeason, KeepBody(keep), "id" to owned.id, "number" to number)
                if (result is ApiResult.Failed) {
                    say(feedback.failed(result.error))
                    return@busy
                }
            }
            val wanted = if (pick == SeasonPick.OFF) "none" else "all"
            if (show.newSeasons != wanted) {
                val result = api.result(LibraryEndpoints.change, TitleChange(newSeasons = wanted), "id" to owned.id)
                if (result is ApiResult.Failed) say(feedback.failed(result.error))
            }
        }
        reloadOwned()
    }

    suspend fun refreshDetails() {
        val owned = current.owned ?: return
        busy("refresh") {
            api
                .result(TitleEndpoints.refresh, "id" to owned.id)
                .onSuccess { say(feedback.next(FeedbackKind.REFRESHING, owned.name)) }
                .onFailure { say(feedback.failed(it)) }
        }
    }

    private suspend fun readFollow() {
        val s = current
        val owned = s.owned
        val following =
            if (owned != null) {
                api.result(TitleEndpoints.followTitle, "id" to owned.id).valueOrNull()?.following
            } else if (s.isShow) {
                api.result(TitleEndpoints.followShow, "tmdb_id" to s.tmdb).valueOrNull()?.following
            } else {
                api.result(TitleEndpoints.followMovie, "tmdb_id" to s.tmdb).valueOrNull()?.following
            }
        mutableState.update { it.copy(following = following ?: false) }
    }

    suspend fun toggleFollow() {
        val s = current
        if (s.followBusy || !s.access.followOn) return
        mutableState.update { it.copy(followBusy = true) }
        val on = !s.following
        val owned = s.owned
        val result: ApiResult<Boolean> =
            when {
                owned != null && on -> api.result(TitleEndpoints.followTitleOn, "id" to owned.id).map { it.following }
                owned != null -> api.result(TitleEndpoints.followTitleOff, "id" to owned.id).map { it.following }
                s.isShow && on -> api.result(TitleEndpoints.followShowOn, "tmdb_id" to s.tmdb).map { it.following }
                s.isShow -> api.result(TitleEndpoints.followShowOff, "tmdb_id" to s.tmdb).map { it.following }
                on -> api.result(TitleEndpoints.followMovieOn, "tmdb_id" to s.tmdb).map { it.following }
                else -> api.result(TitleEndpoints.followMovieOff, "tmdb_id" to s.tmdb).map { it.following }
            }
        when (result) {
            is ApiResult.Ok -> mutableState.update { it.copy(following = result.value, followBusy = false) }
            is ApiResult.Failed -> mutableState.update { it.copy(followBusy = false, feedback = feedback.failed(result.error)) }
        }
    }

    suspend fun moveTo(folderId: Long) {
        val owned = current.owned ?: return
        val label =
            current.folders
                .firstOrNull { it.id == folderId }
                ?.shown
                .orEmpty()
        busy("folder") {
            api
                .result(LibraryEndpoints.move, TitleMove(folderId, apply = true), "id" to owned.id)
                .onSuccess { say(feedback.next(FeedbackKind.MOVING, label)) }
                .onFailure { say(feedback.failed(it)) }
        }
    }

    suspend fun markFailed(downloadId: Long) {
        busy("fail-$downloadId") {
            api
                .result(TitleSupportEndpoints.markFailed, "id" to downloadId)
                .onSuccess { say(feedback.next(FeedbackKind.MARKED_FAILED)) }
                .onFailure { say(feedback.failed(it)) }
        }
        reloadOwned()
    }

    suspend fun loadPoints(slotId: Long) {
        val owned = current.owned ?: return
        if (!current.access.manage || current.points.containsKey(slotId)) return
        api.result(TitleEndpoints.points, "id" to owned.id, "slot" to slotId).onSuccess { v ->
            mutableState.update { it.copy(points = it.points + (slotId to v)) }
        }
    }

    suspend fun addVersion(
        label: String,
        rulesId: Long,
        folderId: Long,
        keepUpToDate: Boolean,
        edition: String,
        audioLanguages: String,
        folderName: String,
    ): Boolean =
        versionWrite(label) { owned ->
            api.result(
                TitleEndpoints.addVersion,
                versionInput(label, rulesId, folderId, keepUpToDate, edition, audioLanguages, folderName),
                "id" to owned.id,
            )
        }

    suspend fun keepAndAdd(
        fileId: Long,
        label: String,
        rulesId: Long,
        folderId: Long,
        keepUpToDate: Boolean,
        edition: String,
        audioLanguages: String,
        folderName: String,
    ): Boolean =
        versionWrite(label) { owned ->
            api.result(
                TitleEndpoints.keepAndAdd,
                versionInput(label, rulesId, folderId, keepUpToDate, edition, audioLanguages, folderName),
                "id" to owned.id,
                "fid" to fileId,
            )
        }

    private fun versionInput(
        label: String,
        rulesId: Long,
        folderId: Long,
        keepUpToDate: Boolean,
        edition: String,
        audioLanguages: String,
        folderName: String,
    ) = VersionInput(
        label = label.trim(),
        pickRulesId = rulesId.takeIf { it > 0 },
        libraryFolderId = folderId.takeIf { it > 0 },
        keepUpToDate = keepUpToDate,
        edition = edition.trim().ifEmpty { null },
        audioLanguages = TitleLogic.languageCodes(audioLanguages),
        folderName = folderName.trim().ifEmpty { null },
    )

    private suspend fun versionWrite(
        label: String,
        call: suspend (OwnedTitle) -> ApiResult<*>,
    ): Boolean {
        val owned = current.owned ?: return false
        var ok = false
        busy("version") {
            when (val result = call(owned)) {
                is ApiResult.Ok -> {
                    ok = true
                    say(feedback.next(FeedbackKind.VERSION_ADDED, label.trim()))
                }
                is ApiResult.Failed -> say(feedback.failed(result.error))
            }
        }
        if (ok) reloadOwned()
        return ok
    }

    suspend fun changeVersion(
        versionId: Long,
        label: String,
        rulesId: Long,
        keepUpToDate: Boolean,
        audioLanguages: String,
    ): Boolean {
        val owned = current.owned ?: return false
        var ok = false
        busy("version") {
            val body =
                VersionAudioChange(
                    label = label.trim(),
                    pickRulesId = rulesId.takeIf { it > 0 },
                    keepUpToDate = keepUpToDate,
                    audioLanguages = TitleLogic.languageCodes(audioLanguages),
                )
            api
                .result(LibraryEndpoints.audio, body, "id" to owned.id, "vid" to versionId)
                .onSuccess {
                    ok = true
                    say(feedback.next(FeedbackKind.VERSION_SAVED, label.trim()))
                }.onFailure { say(feedback.failed(it)) }
        }
        if (ok) reloadOwned()
        return ok
    }

    suspend fun removeVersion(versionId: Long) {
        val owned = current.owned ?: return
        val label =
            owned.versions
                .firstOrNull { it.id == versionId }
                ?.label
                .orEmpty()
        busy("version") {
            api
                .result(TitleEndpoints.removeVersion, "id" to owned.id, "vid" to versionId)
                .onSuccess { say(feedback.next(FeedbackKind.VERSION_REMOVED, label)) }
                .onFailure { say(feedback.failed(it)) }
        }
        reloadOwned()
    }

    suspend fun setEdition(
        fileId: Long,
        edition: String,
    ): Boolean = fileChoice(fileId, FileChoice(edition = edition.trim()))

    suspend fun protect(
        fileId: Long,
        on: Boolean,
    ): Boolean =
        fileChoice(fileId, if (on) FileChoice(isProtected = true) else FileChoice(isProtected = false, acknowledgeProtection = true))

    private suspend fun fileChoice(
        fileId: Long,
        body: FileChoice,
    ): Boolean {
        val owned = current.owned ?: return false
        var ok = false
        busy("file-$fileId") {
            api
                .result(TitleEndpoints.fileChoices, body, "id" to owned.id, "fid" to fileId)
                .onSuccess {
                    ok = true
                    say(feedback.next(FeedbackKind.FILE_SAVED, owned.name))
                }.onFailure { say(feedback.failed(it)) }
        }
        if (ok) reloadOwned()
        return ok
    }

    suspend fun adopt(
        versionId: Long,
        relativePath: String,
    ): Boolean {
        val owned = current.owned ?: return false
        val label =
            owned.versions
                .firstOrNull { it.id == versionId }
                ?.label
                .orEmpty()
        var ok = false
        busy("adopt") {
            api
                .result(TitleEndpoints.adopt, AdoptBody(relativePath.trim()), "id" to owned.id, "vid" to versionId)
                .onSuccess {
                    ok = true
                    say(feedback.next(FeedbackKind.FILE_ADOPTED, label))
                }.onFailure { say(feedback.failed(it)) }
        }
        if (ok) reloadOwned()
        return ok
    }

    suspend fun restoreHeld() {
        val ids = current.held.map { it.id }
        if (ids.isEmpty()) return
        busy("restore") {
            api
                .result(LibraryEndpoints.restore, RestoreRequest(ids))
                .onSuccess { say(feedback.next(FeedbackKind.RESTORED, current.name)) }
                .onFailure { say(feedback.failed(it)) }
        }
        reloadOwned()
    }

    suspend fun loadUsers() {
        if (!current.access.mayHide || current.users.isNotEmpty()) return
        api.result(TitleSupportEndpoints.users).onSuccess { v -> mutableState.update { it.copy(users = v.users) } }
    }

    suspend fun hide(
        genre: String,
        userId: Long,
        whoName: String,
    ): Boolean {
        val s = current
        var ok = false
        busy("hide") {
            val body =
                if (genre.isNotEmpty()) {
                    HideBody(kind = s.kind, genre = genre, userId = userId.takeIf { it > 0 })
                } else {
                    HideBody(kind = s.kind, tmdbId = s.tmdb, name = s.name, userId = userId.takeIf { it > 0 })
                }
            api
                .result(TitleSupportEndpoints.hide, body)
                .onSuccess { hide ->
                    ok = true
                    mutableState.update { st ->
                        st.copy(details = st.details?.let { d -> d.copy(hidden = true, hides = d.hides + hide) })
                    }
                    say(feedback.next(FeedbackKind.HIDDEN, whoName))
                }.onFailure { say(feedback.failed(it)) }
        }
        return ok
    }

    suspend fun unhide(hideId: Long) {
        busy("hide") {
            api
                .result(TitleSupportEndpoints.unhide, "id" to hideId)
                .onSuccess {
                    mutableState.update { st ->
                        st.copy(
                            details =
                                st.details?.let { d ->
                                    val left = d.hides.filter { it.id != hideId }
                                    d.copy(hides = left, hidden = left.isNotEmpty())
                                },
                        )
                    }
                    say(feedback.next(FeedbackKind.SHOWN_AGAIN, current.name))
                }.onFailure { say(feedback.failed(it)) }
        }
    }

    suspend fun trailerStream(): TrailerSource? {
        val s = current
        val key = s.trailer?.key.orEmpty()
        if (s.access.isKid || !s.hasTmdb) return if (key.isNotEmpty()) TrailerSource(youTubeKey = key) else null
        return when (val result = api.result(TitleEndpoints.trailer, "kind" to s.kind, "tmdb_id" to s.tmdb)) {
            is ApiResult.Ok -> {
                val v = result.value
                TrailerSource(
                    hls = if (v.type == "hls") v.url else "",
                    mp4 = if (v.type == "mp4") v.url else v.mp4Url,
                    relay = v.proxyUrl,
                    youTubeKey = v.key.ifEmpty { key },
                )
            }
            is ApiResult.Failed -> if (key.isNotEmpty()) TrailerSource(youTubeKey = key) else null
        }
    }

    fun seasons(): ShowSeasonsPresenter = ShowSeasonsPresenter(api, current.ownedId, current.access.manage) { reloadOwned() }

    fun episode(episode: OwnedEpisode): EpisodePresenter {
        val s = current
        return EpisodePresenter(api, s.ownedId, s.tmdb, episode, s.access.manage, s.access.subtitlesOn) { reloadOwned() }
    }

    fun releases(scope: ReleaseScope): ReleasesPresenter {
        val s = current
        return ReleasesPresenter(api, s.ownedId, scope, s.isShow, s.name, s.movie, s.tmdb) { reloadOwned() }
    }

    fun subtitles(itemId: Long): SubtitlesPresenter {
        val s = current
        return SubtitlesPresenter(api, s.ownedId, itemId, !s.isShow, s.access.settings)
    }

    fun leftovers(): LeftoversPresenter {
        val s = current
        val seasons =
            s.owned
                ?.show
                ?.seasons
                .orEmpty()
                .map { it.number }
        return LeftoversPresenter(api, s.ownedId, s.isShow, s.access.handsOff, seasons) { reloadOwned() }
    }

    fun rename(season: Int): RenamePresenter = RenamePresenter(api, current.ownedId, season) { reloadOwned() }

    fun remove(
        season: Int,
        episodeIds: List<Long>,
    ): RemovePresenter {
        val s = current
        return RemovePresenter(api, s.ownedId, s.name, s.isShow, season, episodeIds, s.access.handsOff) { whole ->
            if (whole) afterRemoved() else reloadOwned()
        }
    }

    fun add(
        choose: Boolean,
        checkId: Long,
        release: TitleRelease?,
    ): AddTitlePresenter {
        val s = current
        return AddTitlePresenter(
            api,
            s.kind,
            s.name,
            s.tmdb,
            s.show,
            s.access.manage,
            s.access.handsOff,
            choose,
            emptyList(),
            checkId,
            release,
        ) { id ->
            afterAdded(id)
        }
    }

    fun addAll(): AddTitlePresenter {
        val s = current
        val parts = s.collectionParts.filter { it.titleId == 0L && !it.inLibrary && (s.access.manage || it.ask == null) }
        return AddTitlePresenter(api, "movie", s.collectionName, s.tmdb, null, s.access.manage, s.access.handsOff, false, parts, 0, null) {
            refresh()
        }
    }

    fun seasonBrowser(thisYear: Int): SeasonBrowserPresenter =
        SeasonBrowserPresenter(api, current.tmdb, current.details?.seasons.orEmpty(), thisYear)

    fun reviews(): ReviewsPresenter = ReviewsPresenter(api, current.kind, current.tmdb)

    private suspend fun afterAdded(id: Long) {
        if (id > 0) mutableState.update { it.copy(libraryId = id) }
        refresh()
    }

    private suspend fun afterRemoved() {
        mutableState.update {
            it.copy(owned = null, tab = TitleTab.OVERVIEW, events = emptyList(), libraryId = if (it.hasTmdb) 0 else it.libraryId)
        }
        if (current.hasTmdb) refresh() else mutableState.update { it.copy(problem = TitleProblem.NOT_IN_LIBRARY) }
    }

    suspend fun watch() =
        coroutineScope {
            val pokes = Channel<Unit>(Channel.CONFLATED)
            launch {
                for (poke in pokes) {
                    delay(LIVE_DEBOUNCE_MILLIS)
                    reloadOwned()
                }
            }
            if (!current.access.manage) {
                launch {
                    while (true) {
                        delay(POLL_MILLIS)
                        val owned = current.owned
                        if (owned != null &&
                            (TitleLogic.activeSlot(owned) != null || (owned.show?.downloading ?: 0) > 0)
                        ) {
                            pokes.trySend(Unit)
                        }
                    }
                }
            }
            api.live.collect { event -> if (concerns(event)) pokes.trySend(Unit) }
        }

    private fun concerns(event: LiveEvent): Boolean {
        val owned = current.owned ?: return false
        val data = event.data as? JsonObject

        fun long(vararg keys: String): Long? = keys.firstNotNullOfOrNull { (data?.get(it) as? JsonPrimitive)?.longOrNull }
        return when (event.topic) {
            LiveEvent.RESYNC, "slot" -> true
            "title" -> long("id", "ID")?.let { it == owned.id } ?: true
            "download" -> long("ID", "id")?.let { id -> owned.slots.any { it.activeDownloadId == id } } ?: false
            "event" -> long("TitleID", "title_id") == owned.id
            else -> false
        }
    }

    private inline fun busy(
        key: String,
        block: () -> Unit,
    ) {
        mutableState.update { it.copy(busy = key) }
        try {
            block()
        } finally {
            mutableState.update { if (it.busy == key) it.copy(busy = "") else it }
        }
    }

    private fun say(next: Feedback) {
        mutableState.update { it.copy(feedback = next) }
    }

    private fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> =
        when (this) {
            is ApiResult.Ok -> ApiResult.Ok(transform(value))
            is ApiResult.Failed -> this
        }

    internal companion object {
        const val LIVE_DEBOUNCE_MILLIS = 400L
        const val POLL_MILLIS = 20_000L

        fun normalKind(kind: String): String =
            when (kind.lowercase()) {
                "show", "tv", "series", "shows" -> "show"
                else -> "movie"
            }
    }
}

data class TrailerSource(
    val hls: String = "",
    val mp4: String = "",
    val relay: String = "",
    val youTubeKey: String = "",
) {
    val youTubeUrl: String get() = if (youTubeKey.isNotEmpty()) "https://www.youtube.com/watch?v=$youTubeKey" else ""

    val playable: Boolean get() = hls.isNotEmpty() || mp4.isNotEmpty() || relay.isNotEmpty()
}

internal fun ApiError.isNeedsForce(): Boolean = code == ApiErrorCode.CONFLICT
