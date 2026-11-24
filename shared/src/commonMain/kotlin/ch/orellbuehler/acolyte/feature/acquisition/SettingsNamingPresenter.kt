package ch.orellbuehler.acolyte.feature.acquisition

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.NamingEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.model.FileSettings
import ch.orellbuehler.acolyte.api.model.SettingsNamingExampleBody
import ch.orellbuehler.acolyte.api.model.SettingsNamingRules
import ch.orellbuehler.acolyte.api.model.SettingsNamingView
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SettingsNamingState(
    val status: LoadStatus = LoadStatus(),
    val allowed: Boolean = true,
    val canClean: Boolean = false,
    internal val saved: SettingsNamingRules = SettingsNamingRules(),
    internal val draft: SettingsNamingRules = SettingsNamingRules(),
    internal val savedFiles: FileSettings = FileSettings(),
    internal val files: FileSettings = FileSettings(),
    internal val typed: Map<SettingsNamingNumber, String> = emptyMap(),
    internal val words: Map<String, String> = emptyMap(),
    val example: String = "",
    val exampleFolder: String = "",
    internal val exampleEpisodes: Map<String, String> = emptyMap(),
    internal val exampleMusic: Map<String, String> = emptyMap(),
    internal val exampleFor: SettingsNamingExampleBody? = null,
    val exampleError: ApiError? = null,
    val exampleLoading: Boolean = false,
    val saving: Boolean = false,
    val saveError: ApiError? = null,
    val savedCount: Int = 0,
    val cleaning: Boolean = false,
    val cleanNote: SettingsNamingCleanNote? = null,
    val cleanDays: Int = 0,
    val cleanCount: Int = 0,
    val cleanError: ApiError? = null,
) {
    val dirty: Boolean get() =
        draft != saved ||
            files != savedFiles ||
            typed.any { (field, text) -> settingsNamingParseNumber(field, text) != numberOf(field, saved, savedFiles) }

    val numbersValid: Boolean get() = SettingsNamingNumber.entries.all { numberError(it) == null }

    val canSave: Boolean get() = dirty && numbersValid && exampleError == null && !saving

    val languages: List<SettingsNamingLanguage> get() = SETTINGS_NAMING_LANGUAGES

    val episodeRows: List<SettingsNamingExampleRow> get() = settingsNamingRows(exampleEpisodes, SETTINGS_NAMING_EPISODE_KEYS)

    val musicRows: List<SettingsNamingExampleRow> get() = settingsNamingRows(exampleMusic, SETTINGS_NAMING_MUSIC_KEYS)

    val values: List<SettingsNamingValue> get() =
        SettingsNamingText.entries.map { SettingsNamingValue(it.wire, text(it)) } +
            SettingsNamingChoice.entries.map { SettingsNamingValue(it.wire, choice(it)) } +
            SettingsNamingNumber.entries.map { SettingsNamingValue(it.wire, number(it)) }

    val switchesOn: List<String> get() = SettingsNamingSwitch.entries.filter { isOn(it) }.map { it.wire }

    fun text(field: SettingsNamingText): String =
        when (field) {
            SettingsNamingText.FILE_PATTERN -> draft.filePattern
            SettingsNamingText.FOLDER_PATTERN -> draft.folderPattern
            SettingsNamingText.SHOW_FOLDER_PATTERN -> draft.showFolderPattern
            SettingsNamingText.SEASON_FOLDER_PATTERN -> draft.seasonFolderPattern
            SettingsNamingText.SPECIALS_FOLDER -> draft.specialsFolder
            SettingsNamingText.EPISODE_PATTERN -> draft.episodePattern
            SettingsNamingText.DAILY_PATTERN -> draft.dailyPattern
            SettingsNamingText.ANIME_PATTERN -> draft.animePattern
            SettingsNamingText.ARTIST_FOLDER_PATTERN -> draft.artistFolderPattern
            SettingsNamingText.ALBUM_FOLDER_PATTERN -> draft.albumFolderPattern
            SettingsNamingText.TRACK_PATTERN -> draft.trackPattern
            SettingsNamingText.MULTI_DISC_TRACK_PATTERN -> draft.multiDiscTrackPattern
            SettingsNamingText.DISC_FOLDER_PATTERN -> draft.discFolderPattern
        }

    fun text(wire: String): String = SettingsNamingText.of(wire)?.let { text(it) }.orEmpty()

    fun isOn(field: SettingsNamingSwitch): Boolean =
        when (field) {
            SettingsNamingSwitch.UNKEEP_MOVIES -> files.unkeepDeletedMovies
            SettingsNamingSwitch.UNKEEP_EPISODES -> files.unkeepDeletedEpisodes
            SettingsNamingSwitch.SKIP_FREE_CHECK -> files.skipFreeSpaceCheck
            SettingsNamingSwitch.GROUP_BY_GENRE -> draft.groupByGenre
            SettingsNamingSwitch.GROUP_BY_LETTER -> draft.groupByLetter
            SettingsNamingSwitch.SHOW_GROUP_BY_LETTER -> draft.showGroupByLetter
            SettingsNamingSwitch.SEASON_FOLDERS -> draft.seasonFolders
            SettingsNamingSwitch.DISC_FOLDERS -> draft.discFolders
            SettingsNamingSwitch.READ_TAGS -> draft.musicReadTags
            SettingsNamingSwitch.RENAME_MOVIES -> draft.renameOnImport
            SettingsNamingSwitch.RENAME_SHOWS -> draft.showRenameOnImport
            SettingsNamingSwitch.RENAME_MUSIC -> draft.musicRenameOnImport
        }

    fun isOn(wire: String): Boolean = SettingsNamingSwitch.of(wire)?.let { isOn(it) } ?: false

    fun choice(field: SettingsNamingChoice): String =
        when (field) {
            SettingsNamingChoice.COLON -> draft.colon
            SettingsNamingChoice.ILLEGAL -> draft.illegal
            SettingsNamingChoice.SHOW_COLON -> draft.showColon
            SettingsNamingChoice.SHOW_ILLEGAL -> draft.showIllegal
            SettingsNamingChoice.MUSIC_COLON -> draft.musicColon
            SettingsNamingChoice.MUSIC_ILLEGAL -> draft.musicIllegal
            SettingsNamingChoice.MULTI_EPISODE -> draft.multiEpisodeStyle
            SettingsNamingChoice.AIR_DATE -> draft.airDateStyle
            SettingsNamingChoice.ABSOLUTE_DIGITS -> draft.absoluteDigits.toString()
            SettingsNamingChoice.TITLE_LANGUAGE -> draft.titleLanguage
        }

    fun choice(wire: String): String = SettingsNamingChoice.of(wire)?.let { choice(it) }.orEmpty()

    fun number(field: SettingsNamingNumber): String = typed[field] ?: numberOf(field, draft, files).toString()

    fun number(wire: String): String = SettingsNamingNumber.of(wire)?.let { number(it) }.orEmpty()

    fun numberError(field: SettingsNamingNumber): SettingsNamingNumberError? = settingsNamingNumberError(field, number(field))

    fun numberErrorKey(wire: String): String =
        SettingsNamingNumber
            .of(wire)
            ?.let { numberError(it) }
            ?.name
            ?.lowercase()
            ?.replace("_", "")
            .orEmpty()

    fun words(
        field: SettingsNamingText,
        query: String,
    ): List<SettingsNamingWord> = settingsNamingWords(words, field.wordSet, query)

    fun words(
        wire: String,
        query: String,
    ): List<SettingsNamingWord> = SettingsNamingText.of(wire)?.let { words(it, query) }.orEmpty()

    fun exampleFor(field: SettingsNamingText): String {
        val episodes = exampleEpisodes
        val music = exampleMusic
        return when (field) {
            SettingsNamingText.FILE_PATTERN, SettingsNamingText.FOLDER_PATTERN ->
                if (exampleFolder.isEmpty()) example else "$exampleFolder/$example"
            SettingsNamingText.SHOW_FOLDER_PATTERN -> episodes["show_folder"].orEmpty()
            SettingsNamingText.SEASON_FOLDER_PATTERN, SettingsNamingText.EPISODE_PATTERN -> episodes["episode"].orEmpty()
            SettingsNamingText.SPECIALS_FOLDER -> episodes["special"].orEmpty()
            SettingsNamingText.DAILY_PATTERN -> episodes["daily"].orEmpty()
            SettingsNamingText.ANIME_PATTERN -> episodes["anime"].orEmpty()
            SettingsNamingText.ARTIST_FOLDER_PATTERN -> music["artist_folder"].orEmpty()
            SettingsNamingText.ALBUM_FOLDER_PATTERN -> music["album_folder"].orEmpty()
            SettingsNamingText.TRACK_PATTERN -> music["track"].orEmpty()
            SettingsNamingText.MULTI_DISC_TRACK_PATTERN, SettingsNamingText.DISC_FOLDER_PATTERN -> music["multi_disc"].orEmpty()
        }
    }

    fun exampleFor(wire: String): String = SettingsNamingText.of(wire)?.let { exampleFor(it) }.orEmpty()

    internal fun exampleBody(): SettingsNamingExampleBody? = if (status.phase == Phase.READY) draft.toExampleBody() else null
}

private fun numberOf(
    field: SettingsNamingNumber,
    rules: SettingsNamingRules,
    files: FileSettings,
): Int =
    when (field) {
        SettingsNamingNumber.HOLDING_DAYS -> files.holdingDays
        SettingsNamingNumber.MIN_FREE_MB -> files.minFreeMb
        SettingsNamingNumber.ADOPT_SCAN_LIMIT -> rules.adoptScanLimit
        SettingsNamingNumber.MUSIC_ADOPT_SCAN_LIMIT -> rules.musicAdoptScanLimit
    }

internal fun SettingsNamingRules.toExampleBody(): SettingsNamingExampleBody =
    SettingsNamingExampleBody(
        filePattern = filePattern,
        folderPattern = folderPattern,
        colon = colon,
        illegal = illegal,
        renameOnImport = renameOnImport,
        groupByGenre = groupByGenre,
        groupByLetter = groupByLetter,
        showGroupByLetter = showGroupByLetter,
        showColon = showColon,
        showIllegal = showIllegal,
        musicColon = musicColon,
        musicIllegal = musicIllegal,
        titleLanguage = titleLanguage,
        showFolderPattern = showFolderPattern,
        seasonFolders = seasonFolders,
        seasonFolderPattern = seasonFolderPattern,
        specialsFolder = specialsFolder,
        episodePattern = episodePattern,
        dailyPattern = dailyPattern,
        animePattern = animePattern,
        multiEpisodeStyle = multiEpisodeStyle,
        airDateStyle = airDateStyle,
        absoluteDigits = absoluteDigits,
        artistFolderPattern = artistFolderPattern,
        albumFolderPattern = albumFolderPattern,
        trackPattern = trackPattern,
        multiDiscTrackPattern = multiDiscTrackPattern,
        discFolders = discFolders,
        discFolderPattern = discFolderPattern,
    )

class SettingsNamingPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
) {
    private val mutableState = MutableStateFlow(SettingsNamingState())
    private val lock = Mutex()
    private var sequence = 0

    val state: StateFlow<SettingsNamingState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            val me = account()
            if (me == null || !me.mayOpen(AcquisitionPage.NAMING)) {
                mutableState.update { it.copy(status = LoadStatus.ready, allowed = false) }
                return
            }
            mutableState.update { it.copy(status = it.status.started()) }
            coroutineScope {
                val naming = async { api.result(NamingEndpoints.settings) }
                val files = async { api.result(LibraryEndpoints.fileSettings) }
                val first = naming.await()
                val second = files.await()
                when {
                    first is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(first.error)) }
                    second is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(second.error)) }
                    first is ApiResult.Ok && second is ApiResult.Ok ->
                        mutableState.update {
                            it
                                .withView(first.value)
                                .copy(
                                    status = LoadStatus.ready,
                                    canClean = me.can(Need.MANAGE_LIBRARY),
                                    savedFiles = second.value,
                                    files = second.value,
                                    typed = emptyMap(),
                                    saveError = null,
                                )
                        }
                }
            }
        }
    }

    fun setText(
        field: SettingsNamingText,
        text: String,
    ) = edit {
        when (field) {
            SettingsNamingText.FILE_PATTERN -> it.copy(filePattern = text)
            SettingsNamingText.FOLDER_PATTERN -> it.copy(folderPattern = text)
            SettingsNamingText.SHOW_FOLDER_PATTERN -> it.copy(showFolderPattern = text)
            SettingsNamingText.SEASON_FOLDER_PATTERN -> it.copy(seasonFolderPattern = text)
            SettingsNamingText.SPECIALS_FOLDER -> it.copy(specialsFolder = text)
            SettingsNamingText.EPISODE_PATTERN -> it.copy(episodePattern = text)
            SettingsNamingText.DAILY_PATTERN -> it.copy(dailyPattern = text)
            SettingsNamingText.ANIME_PATTERN -> it.copy(animePattern = text)
            SettingsNamingText.ARTIST_FOLDER_PATTERN -> it.copy(artistFolderPattern = text)
            SettingsNamingText.ALBUM_FOLDER_PATTERN -> it.copy(albumFolderPattern = text)
            SettingsNamingText.TRACK_PATTERN -> it.copy(trackPattern = text)
            SettingsNamingText.MULTI_DISC_TRACK_PATTERN -> it.copy(multiDiscTrackPattern = text)
            SettingsNamingText.DISC_FOLDER_PATTERN -> it.copy(discFolderPattern = text)
        }
    }

    fun setText(
        wire: String,
        text: String,
    ) {
        SettingsNamingText.of(wire)?.let { setText(it, text) }
    }

    fun insertWord(
        field: SettingsNamingText,
        word: String,
        start: Int,
        end: Int,
    ): Int {
        if (!field.insertable) return start
        val result = settingsNamingInsert(mutableState.value.text(field), start, end, word)
        setText(field, result.text)
        return result.cursor
    }

    fun insertWord(
        wire: String,
        word: String,
        start: Int,
        end: Int,
    ): Int = SettingsNamingText.of(wire)?.let { insertWord(it, word, start, end) } ?: start

    fun setSwitch(
        field: SettingsNamingSwitch,
        on: Boolean,
    ) = mutableState.update { s ->
        val files =
            when (field) {
                SettingsNamingSwitch.UNKEEP_MOVIES -> s.files.copy(unkeepDeletedMovies = on)
                SettingsNamingSwitch.UNKEEP_EPISODES -> s.files.copy(unkeepDeletedEpisodes = on)
                SettingsNamingSwitch.SKIP_FREE_CHECK -> s.files.copy(skipFreeSpaceCheck = on)
                else -> s.files
            }
        val draft =
            when (field) {
                SettingsNamingSwitch.GROUP_BY_GENRE -> s.draft.copy(groupByGenre = on, groupByLetter = s.draft.groupByLetter && !on)
                SettingsNamingSwitch.GROUP_BY_LETTER -> s.draft.copy(groupByLetter = on, groupByGenre = s.draft.groupByGenre && !on)
                SettingsNamingSwitch.SHOW_GROUP_BY_LETTER -> s.draft.copy(showGroupByLetter = on)
                SettingsNamingSwitch.SEASON_FOLDERS -> s.draft.copy(seasonFolders = on)
                SettingsNamingSwitch.DISC_FOLDERS -> s.draft.copy(discFolders = on)
                SettingsNamingSwitch.READ_TAGS -> s.draft.copy(musicReadTags = on)
                SettingsNamingSwitch.RENAME_MOVIES -> s.draft.copy(renameOnImport = on)
                SettingsNamingSwitch.RENAME_SHOWS -> s.draft.copy(showRenameOnImport = on)
                SettingsNamingSwitch.RENAME_MUSIC -> s.draft.copy(musicRenameOnImport = on)
                else -> s.draft
            }
        s.copy(draft = draft, files = files, saveError = null)
    }

    fun setSwitch(
        wire: String,
        on: Boolean,
    ) {
        SettingsNamingSwitch.of(wire)?.let { setSwitch(it, on) }
    }

    fun setChoice(
        field: SettingsNamingChoice,
        value: String,
    ) = edit {
        when (field) {
            SettingsNamingChoice.COLON -> it.copy(colon = value)
            SettingsNamingChoice.ILLEGAL -> it.copy(illegal = value)
            SettingsNamingChoice.SHOW_COLON -> it.copy(showColon = value)
            SettingsNamingChoice.SHOW_ILLEGAL -> it.copy(showIllegal = value)
            SettingsNamingChoice.MUSIC_COLON -> it.copy(musicColon = value)
            SettingsNamingChoice.MUSIC_ILLEGAL -> it.copy(musicIllegal = value)
            SettingsNamingChoice.MULTI_EPISODE -> it.copy(multiEpisodeStyle = value)
            SettingsNamingChoice.AIR_DATE -> it.copy(airDateStyle = value)
            SettingsNamingChoice.ABSOLUTE_DIGITS -> it.copy(absoluteDigits = value.toIntOrNull() ?: it.absoluteDigits)
            SettingsNamingChoice.TITLE_LANGUAGE -> it.copy(titleLanguage = value)
        }
    }

    fun setChoice(
        wire: String,
        value: String,
    ) {
        SettingsNamingChoice.of(wire)?.let { setChoice(it, value) }
    }

    fun setNumber(
        field: SettingsNamingNumber,
        text: String,
    ) = mutableState.update { s ->
        val digits = text.filter { it.isDigit() }.take(MAX_DIGITS)
        val value = settingsNamingParseNumber(field, digits)
        val draft =
            when {
                value == null -> s.draft
                field == SettingsNamingNumber.ADOPT_SCAN_LIMIT -> s.draft.copy(adoptScanLimit = value)
                field == SettingsNamingNumber.MUSIC_ADOPT_SCAN_LIMIT -> s.draft.copy(musicAdoptScanLimit = value)
                else -> s.draft
            }
        val files =
            when {
                value == null -> s.files
                field == SettingsNamingNumber.HOLDING_DAYS -> s.files.copy(holdingDays = value)
                field == SettingsNamingNumber.MIN_FREE_MB -> s.files.copy(minFreeMb = value)
                else -> s.files
            }
        s.copy(draft = draft, files = files, typed = s.typed + (field to digits), saveError = null)
    }

    fun setNumber(
        wire: String,
        text: String,
    ) {
        SettingsNamingNumber.of(wire)?.let { setNumber(it, text) }
    }

    fun applyStyle(style: SettingsNamingStyle) =
        edit {
            it.copy(
                folderPattern = style.folder,
                filePattern = style.file,
                showFolderPattern = style.show,
                episodePattern = style.episode,
            )
        }

    fun applyStyle(wire: String) {
        SettingsNamingStyle.of(wire)?.let { applyStyle(it) }
    }

    fun discard() =
        mutableState.update {
            it.copy(draft = it.saved, files = it.savedFiles, typed = emptyMap(), saveError = null)
        }

    suspend fun save() {
        val s = mutableState.value
        if (!s.canSave) return
        mutableState.update { it.copy(saving = true, saveError = null) }
        val view =
            when (val result = api.result(NamingEndpoints.save, s.draft)) {
                is ApiResult.Ok -> result.value
                is ApiResult.Failed -> {
                    mutableState.update { it.copy(saving = false, saveError = result.error) }
                    return
                }
            }
        mutableState.update {
            it.withView(view).copy(
                typed =
                    it.typed.filterKeys { field ->
                        field == SettingsNamingNumber.HOLDING_DAYS ||
                            field == SettingsNamingNumber.MIN_FREE_MB
                    },
            )
        }
        when (val result = api.result(NamingEndpoints.saveFiles, s.files)) {
            is ApiResult.Ok ->
                mutableState.update {
                    it.copy(
                        saving = false,
                        savedFiles = result.value,
                        files = result.value,
                        typed = emptyMap(),
                        savedCount = it.savedCount + 1,
                    )
                }
            is ApiResult.Failed -> mutableState.update { it.copy(saving = false, saveError = result.error) }
        }
    }

    suspend fun cleanHolding() {
        val s = mutableState.value
        if (!s.canClean || s.cleaning) return
        mutableState.update { it.copy(cleaning = true, cleanError = null) }
        when (val result = api.result(NamingEndpoints.cleanHolding)) {
            is ApiResult.Ok -> {
                val days = mutableState.value.savedFiles.holdingDays
                mutableState.update {
                    it.copy(
                        cleaning = false,
                        cleanNote = if (days > 0) SettingsNamingCleanNote.REMOVING else SettingsNamingCleanNote.NOTHING,
                        cleanDays = days,
                        cleanCount = it.cleanCount + 1,
                    )
                }
            }
            is ApiResult.Failed -> mutableState.update { it.copy(cleaning = false, cleanError = result.error) }
        }
    }

    suspend fun followExample() {
        mutableState
            .map { it.exampleBody() }
            .distinctUntilChanged()
            .collectLatest { body ->
                val s = mutableState.value
                if (body == null || (body == s.exampleFor && s.exampleError == null)) return@collectLatest
                delay(EXAMPLE_DEBOUNCE_MILLIS)
                val mine = ++sequence
                mutableState.update { it.copy(exampleLoading = true) }
                val result = api.result(NamingEndpoints.example, body)
                if (mine != sequence) return@collectLatest
                when (result) {
                    is ApiResult.Ok ->
                        mutableState.update {
                            it.copy(
                                example = result.value.example,
                                exampleFolder = result.value.exampleFolder,
                                exampleEpisodes = result.value.exampleEpisodes,
                                exampleMusic = result.value.exampleMusic,
                                exampleFor = body,
                                exampleError = null,
                                exampleLoading = false,
                            )
                        }
                    is ApiResult.Failed -> mutableState.update { it.copy(exampleError = result.error, exampleLoading = false) }
                }
            }
    }

    private fun edit(transform: (SettingsNamingRules) -> SettingsNamingRules) =
        mutableState.update { it.copy(draft = transform(it.draft), saveError = null) }

    private fun SettingsNamingState.withView(view: SettingsNamingView): SettingsNamingState =
        copy(
            saved = view.naming,
            draft = view.naming,
            words = view.words.ifEmpty { words },
            example = view.example,
            exampleFolder = view.exampleFolder,
            exampleEpisodes = view.exampleEpisodes,
            exampleMusic = view.exampleMusic,
            exampleFor = view.naming.toExampleBody(),
            exampleError = null,
            exampleLoading = false,
        )

    private companion object {
        const val EXAMPLE_DEBOUNCE_MILLIS = 250L
        const val MAX_DIGITS = 7
    }
}
