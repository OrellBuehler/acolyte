package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.model.LibraryFolder
import ch.orellbuehler.acolyte.api.model.OwnedDownload
import ch.orellbuehler.acolyte.api.model.OwnedEpisode
import ch.orellbuehler.acolyte.api.model.OwnedEpisodeSlot
import ch.orellbuehler.acolyte.api.model.OwnedFile
import ch.orellbuehler.acolyte.api.model.OwnedQuality
import ch.orellbuehler.acolyte.api.model.OwnedSeason
import ch.orellbuehler.acolyte.api.model.OwnedShow
import ch.orellbuehler.acolyte.api.model.OwnedSlot
import ch.orellbuehler.acolyte.api.model.OwnedTitle
import ch.orellbuehler.acolyte.api.model.PickRule
import ch.orellbuehler.acolyte.api.model.SubtitleWant
import ch.orellbuehler.acolyte.api.model.TitleKindDefaults
import ch.orellbuehler.acolyte.api.model.TitleLeftover
import ch.orellbuehler.acolyte.api.model.TitleLeftoverGuess
import ch.orellbuehler.acolyte.api.model.TitleRelease
import ch.orellbuehler.acolyte.api.model.TitleReleaseRead
import ch.orellbuehler.acolyte.api.model.TitleReleaseReason
import ch.orellbuehler.acolyte.api.model.TitleScore
import ch.orellbuehler.acolyte.api.model.TitleVideo
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.title.AirWord
import ch.orellbuehler.acolyte.feature.title.EpisodeTone
import ch.orellbuehler.acolyte.feature.title.LabelProblem
import ch.orellbuehler.acolyte.feature.title.LibraryKind
import ch.orellbuehler.acolyte.feature.title.LibraryLabel
import ch.orellbuehler.acolyte.feature.title.MatchEpisode
import ch.orellbuehler.acolyte.feature.title.ReleaseSort
import ch.orellbuehler.acolyte.feature.title.SeasonBrief
import ch.orellbuehler.acolyte.feature.title.SeasonPick
import ch.orellbuehler.acolyte.feature.title.TitleLogic
import ch.orellbuehler.acolyte.feature.title.VerdictKind
import ch.orellbuehler.acolyte.feature.title.WishProblem
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TitleLogicTest {
    private fun season(
        n: Int,
        keep: Boolean = true,
        episodes: Int = 10,
        aired: Int = 10,
        have: Int = 0,
        notOut: Int = 0,
    ) = OwnedSeason(number = n, keepUpToDate = keep, episodes = episodes, aired = aired, inLibrary = have, notOutYet = notOut)

    @Test
    fun showStatusCountsAiredEpisodes() {
        val show = OwnedShow(seasons = listOf(season(1, have = 10), season(2, aired = 4, have = 2, notOut = 6)), episodes = 20)
        val status = TitleLogic.libraryStatus(OwnedTitle(kind = "show", show = show.copy(inLibrary = 12)))
        assertEquals(LibraryKind.HAVE, status.kind)
        assertEquals(LibraryLabel.EPISODES_OF, status.label)
        assertEquals(12, status.count)
        assertEquals(14, status.total)
        val done = TitleLogic.libraryStatus(OwnedTitle(kind = "show", show = show.copy(inLibrary = 14)))
        assertEquals(LibraryLabel.IN_LIBRARY, done.label)
        val downloading = TitleLogic.libraryStatus(OwnedTitle(kind = "show", show = show.copy(downloading = 2, failed = 1)))
        assertEquals(LibraryKind.DOWNLOADING, downloading.kind)
        assertEquals(2, downloading.count)
        assertEquals(Tone.INFO, downloading.tone)
        val missing = TitleLogic.libraryStatus(OwnedTitle(kind = "show", show = show.copy(inLibrary = 0)))
        assertEquals(LibraryKind.MISSING, missing.kind)
    }

    @Test
    fun movieStatusFollowsSlots() {
        val file = OwnedFile(id = 3, size = 10)
        assertEquals(LibraryLabel.NOT_DOWNLOADED, TitleLogic.libraryStatus(OwnedTitle(slots = listOf(OwnedSlot(state = "wanted")))).label)
        assertEquals(
            LibraryLabel.IN_LIBRARY,
            TitleLogic.libraryStatus(OwnedTitle(slots = listOf(OwnedSlot(state = "in_library", file = file)))).label,
        )
        assertEquals(LibraryLabel.DOWNLOADED, TitleLogic.libraryStatus(OwnedTitle(slots = listOf(OwnedSlot(state = "in_library")))).label)
        assertEquals(LibraryKind.FAILED, TitleLogic.libraryStatus(OwnedTitle(slots = listOf(OwnedSlot(state = "failed")))).kind)
        val active = OwnedSlot(state = "downloading", download = OwnedDownload(progress = 0.426, active = true))
        val status = TitleLogic.libraryStatus(OwnedTitle(slots = listOf(active)))
        assertEquals(LibraryLabel.DOWNLOADING_PERCENT, status.label)
        assertEquals(42, status.count)
    }

    @Test
    fun qualityBadgesHideHdNextTo4k() {
        val uhd =
            OwnedFile(
                quality = OwnedQuality(resolution = 2160, hdr = listOf("dv", "hdr10")),
                mediaInfo =
                    JsonObject(
                        mapOf(
                            "audio_codec" to JsonPrimitive("TrueHD Atmos"),
                            "subtitle_languages" to JsonArray(listOf(JsonPrimitive("en"))),
                        ),
                    ),
            )
        val hd = OwnedFile(quality = OwnedQuality(resolution = 1080))
        assertEquals(listOf("4K", "Dolby Vision", "HDR", "Atmos", "CC"), TitleLogic.qualityBadges(listOf(uhd, hd)))
        assertEquals(listOf("HD"), TitleLogic.qualityBadges(listOf(hd)))
    }

    @Test
    fun fileMediaInfoReadsLanguages() {
        val file =
            OwnedFile(
                relativePath = "Movie (2020)/Movie.2020.mkv",
                quality = OwnedQuality(resolution = 1080),
                mediaInfo = JsonObject(mapOf("audio_languages" to JsonPrimitive("en/de"), "edition" to JsonPrimitive("Director's Cut"))),
            )
        assertEquals("Movie.2020.mkv", file.fileName)
        assertEquals(listOf("EN", "DE"), file.audioLanguages)
        assertEquals("Director's Cut", file.edition)
        assertEquals("1080p", file.resolutionLabel)
    }

    @Test
    fun trailerPrefersOfficialTrailerInOwnLanguage() {
        val videos =
            listOf(
                TitleVideo(key = "teaserKey01", type = "Teaser", official = true, language = "de"),
                TitleVideo(key = "trailerEn01", type = "Trailer", official = true, language = "en"),
                TitleVideo(key = "trailerDe01", type = "Trailer", official = true, language = "de"),
                TitleVideo(key = "clipKey0001", type = "Clip", official = true, language = "de"),
                TitleVideo(key = "bad key", type = "Trailer", official = true, language = "de"),
            )
        assertEquals("trailerDe01", TitleLogic.pickTrailer(videos, "de-CH")?.key)
        assertEquals("trailerEn01", TitleLogic.pickTrailer(videos, "fr")?.key)
        assertEquals(listOf("trailerDe01", "teaserKey01", "trailerEn01"), TitleLogic.trailerRow(videos, "de").map { it.key })
    }

    @Test
    fun scoresAreCleanedAndWorded() {
        val scores =
            listOf(
                TitleScore("trakt", 7.84, 10.0, 200),
                TitleScore("imdb", 8.0, 10.0, 5000),
                TitleScore("metacritic", 71.4, 100.0, 30),
                TitleScore("rt_critics", 93.0, 100.0, 120),
                TitleScore("unknown", 5.0, 10.0, 10),
                TitleScore("letterboxd", 0.0, 5.0, 10),
            )
        val clean = TitleLogic.cleanScores(scores)
        assertEquals(listOf("imdb", "rt_critics", "metacritic", "trakt"), clean.map { it.source })
        assertEquals(listOf("8.0", "93%", "71", "7.8"), clean.map { TitleLogic.scoreText(it) })
        val (shown, tmdb) = TitleLogic.shownScores(clean, 7.2)
        assertEquals(listOf("imdb", "rt_critics"), shown.map { it.source })
        assertNull(tmdb)
        assertEquals(listOf("trakt"), TitleLogic.shownScores(listOf(clean.last()), 0.0).first.map { it.source })
    }

    @Test
    fun seasonsToAskLeaveOutKeptAndComplete() {
        val owned =
            OwnedTitle(
                kind = "show",
                keepUpToDate = true,
                show = OwnedShow(seasons = listOf(season(1, keep = false, have = 10), season(2, keep = true), season(3, keep = false))),
            )
        assertEquals(listOf(3, 4), TitleLogic.seasonsToAsk(listOf(0, 1, 2, 3, 4), owned))
        assertEquals(listOf(2, 3, 4), TitleLogic.seasonsToAsk(listOf(0, 1, 2, 3, 4), owned.copy(keepUpToDate = false)))
    }

    @Test
    fun newestSeasonSkipsSpecialsAndEmptySeasons() {
        val seasons = listOf(SeasonBrief(0, episodes = 3), SeasonBrief(1, episodes = 8), SeasonBrief(2, episodes = 8), SeasonBrief(3))
        assertEquals(2, TitleLogic.newestSeason(seasons))
        assertEquals(listOf(3, 2, 1, 0), TitleLogic.seasonOrder(listOf(0, 1, 2, 3)))
    }

    @Test
    fun episodeTones() {
        assertEquals(EpisodeTone.HAVE, TitleLogic.episodeTone("in_library", false))
        assertEquals(EpisodeTone.HAVE, TitleLogic.episodeTone("better_wanted", true))
        assertEquals(EpisodeTone.DOWNLOADING, TitleLogic.episodeTone("downloading", true))
        assertEquals(EpisodeTone.FAILED, TitleLogic.episodeTone("failed", false))
        assertEquals(EpisodeTone.MISSING, TitleLogic.episodeTone("wanted", false))
        val episode = OwnedEpisode(slots = listOf(OwnedEpisodeSlot(state = "wanted", fileId = 4)))
        assertEquals(EpisodeTone.HAVE, TitleLogic.episodeTone(episode))
        assertEquals("1080p WEB", TitleLogic.epQuality(OwnedFile(quality = OwnedQuality(resolution = 1080, source = "web_dl"))))
    }

    @Test
    fun seasonPickAndPlan() {
        val all = listOf(season(0, keep = false), season(1), season(2))
        assertEquals(SeasonPick.ALL, TitleLogic.seasonPick(all, "all"))
        val latest = listOf(season(1, keep = false), season(2))
        assertEquals(SeasonPick.LATEST, TitleLogic.seasonPick(latest, "all"))
        assertEquals(SeasonPick.OFF, TitleLogic.seasonPick(listOf(season(1, keep = false)), "none"))
        assertEquals(SeasonPick.CUSTOM, TitleLogic.seasonPick(latest, "none"))
        assertEquals(listOf(1 to false), TitleLogic.seasonKeepPlan(all, SeasonPick.LATEST))
        assertEquals(listOf(1 to true), TitleLogic.seasonKeepPlan(latest, SeasonPick.ALL))
    }

    @Test
    fun showHints() {
        assertTrue(TitleLogic.showOffHint(false, listOf(season(1))))
        assertFalse(TitleLogic.showOffHint(true, listOf(season(1))))
        assertTrue(TitleLogic.getAllMissingShown(true, listOf(season(1, have = 4))))
        assertFalse(TitleLogic.getAllMissingShown(true, listOf(season(1, have = 4, notOut = 6))))
    }

    @Test
    fun airWords() {
        assertEquals(AirWord.TODAY, TitleLogic.airWord("2026-11-24", "2026-11-24"))
        assertEquals(AirWord.YESTERDAY, TitleLogic.airWord("2026-11-23T20:00:00Z", "2026-11-24"))
        assertEquals(AirWord.TOMORROW, TitleLogic.airWord("2026-11-25", "2026-11-24"))
        assertEquals(AirWord.WEEKDAY, TitleLogic.airWord("2026-11-28", "2026-11-24"))
        assertEquals(AirWord.DATE, TitleLogic.airWord("2026-10-02", "2026-11-24"))
        assertEquals(AirWord.DATE_WITH_YEAR, TitleLogic.airWord("2027-01-02", "2026-11-24"))
        assertEquals(AirWord.NONE, TitleLogic.airWord("", "2026-11-24"))
        assertEquals(20416L, TitleLogic.epochDay("2025-11-24"))
        assertEquals(0L, TitleLogic.epochDay("1970-01-01"))
    }

    private fun release(
        id: Long,
        outcome: String = "passed_over",
        state: String = "judged",
        passed: Boolean = true,
        rank: Int = 0,
        points: Int = 0,
        resolution: Int = 1080,
        source: String = "web_dl",
        size: Long = 1,
        seeders: Int? = null,
        published: String = "",
    ) = TitleRelease(
        id = id,
        name = "Show.S01.$id",
        outcome = outcome,
        state = state,
        passed = passed,
        rank = rank,
        points = points,
        size = size,
        seeders = seeders,
        publishedAt = published,
        sourceName = "Source $id",
        read = TitleReleaseRead(resolution = resolution, source = source, languages = listOf("en")),
    )

    @Test
    fun verdictsAndTones() {
        assertEquals(VerdictKind.TURNED_DOWN, TitleLogic.verdict(release(1, state = "turned_down", outcome = "chosen")).kind)
        assertEquals(Tone.INFO, TitleLogic.verdict(release(1, state = "chosen")).tone)
        assertEquals(VerdictKind.WOULD_CHOOSE, TitleLogic.verdict(release(1, outcome = "chosen")).kind)
        assertEquals(Tone.OK, TitleLogic.verdict(release(1, outcome = "chosen")).tone)
        assertEquals(VerdictKind.WOULD_HOLD, TitleLogic.verdict(release(1, outcome = "held")).kind)
        assertEquals(Tone.FAIL, TitleLogic.verdict(release(1, outcome = "refused")).tone)
        assertEquals(VerdictKind.PASSED_OVER, TitleLogic.verdict(release(1)).kind)
        val reasons =
            listOf(
                TitleReleaseReason(effect = "add", text = "a"),
                TitleReleaseReason(effect = "warn", text = "w"),
                TitleReleaseReason(effect = "fail", text = "f"),
                TitleReleaseReason(effect = "rank", text = "r"),
            )
        assertEquals(listOf("f", "r", "w"), TitleLogic.keyReasons(reasons).map { it.text })
    }

    @Test
    fun releaseSortsAndFilters() {
        val list =
            listOf(
                release(1, points = 10, rank = 2, resolution = 2160, source = "remux", size = 50, seeders = 3, published = "2026-01-01"),
                release(2, outcome = "chosen", points = 5, rank = 1, size = 10, seeders = null, published = "2026-03-01"),
                release(3, passed = false, outcome = "refused", points = 30, resolution = 720, size = 5, seeders = 40),
            )
        assertEquals(listOf(2L, 1L, 3L), TitleLogic.sortReleases(list, ReleaseSort.BEST).map { it.id })
        assertEquals(listOf(3L, 1L, 2L), TitleLogic.sortReleases(list, ReleaseSort.POINTS).map { it.id })
        assertEquals(listOf(1L, 2L, 3L), TitleLogic.sortReleases(list, ReleaseSort.QUALITY).map { it.id })
        assertEquals(listOf(1L, 2L, 3L), TitleLogic.sortReleases(list, ReleaseSort.SIZE).map { it.id })
        assertEquals(listOf(3L, 1L, 2L), TitleLogic.sortReleases(list, ReleaseSort.SEEDERS).map { it.id })
        assertEquals(listOf(2L, 1L, 3L), TitleLogic.sortReleases(list, ReleaseSort.AGE).map { it.id })
        assertEquals(listOf(2160, 1080, 720), TitleLogic.resolutionsOf(list))
        assertEquals(listOf("en"), TitleLogic.languagesOf(list))
        assertTrue(TitleLogic.nameMatches("The.Show.S01E02.1080p-GRP", "show 1080p"))
        assertFalse(TitleLogic.nameMatches("The.Show.S01E02", "show 720p"))
        assertEquals(listOf("4K", "Remux", "EN"), TitleLogic.readTags(list[0]))
    }

    @Test
    fun startChoicesUseSavedDefaultsWhileOnOffer() {
        val folders =
            listOf(
                LibraryFolder(1, "/movies", "movie", "Movies", 7, health = "ok"),
                LibraryFolder(2, "/uhd", "movie", "4K movies", 0, health = "ok"),
            )
        val rules =
            listOf(
                PickRule(7, "Standard", emptyList(), true),
                PickRule(8, "4K", listOf("movie"), true),
                PickRule(9, "Shows", listOf("show")),
            )
        val start = TitleLogic.startChoices(TitleKindDefaults(2, 8, false, true), folders, rules, "movie", builtInSearch = true)
        assertEquals(2, start.folderId)
        assertEquals(8, start.rulesId)
        assertFalse(start.keepUpToDate)
        val fallback = TitleLogic.startChoices(TitleKindDefaults(99, 99), folders, rules, "movie", builtInSearch = true)
        assertEquals(1, fallback.folderId)
        assertEquals(7, fallback.rulesId)
        val second = TitleLogic.secondVersion(emptyList(), TitleLogic.rulesFor(rules, "movie"), folders, 1)
        assertEquals("4K", second.label)
        assertEquals(8, second.rulesId)
        assertEquals(2, second.folderId)
        assertEquals("", TitleLogic.secondVersion(listOf("4k"), rules, folders, 1).label)
    }

    @Test
    fun versionLabelRules() {
        assertEquals(LabelProblem.EMPTY, TitleLogic.labelProblem("  ", emptyList()))
        assertEquals(LabelProblem.BAD_CHARACTER, TitleLogic.labelProblem("4K/HDR", emptyList()))
        assertEquals(LabelProblem.TRAILING_DOT, TitleLogic.labelProblem("Cut.", emptyList()))
        assertEquals(LabelProblem.DEVICE_NAME, TitleLogic.labelProblem("con", emptyList()))
        assertEquals(LabelProblem.TAKEN, TitleLogic.labelProblem("Main", listOf("main")))
        assertEquals(LabelProblem.TOO_LONG, TitleLogic.labelProblem("x".repeat(41), emptyList()))
        assertNull(TitleLogic.labelProblem("4K", listOf("Main")))
        assertEquals(LabelProblem.BAD_CHARACTER, TitleLogic.editionProblem("Cut {x}"))
        assertNull(TitleLogic.editionProblem(""))
        assertEquals(listOf("en", "ja"), TitleLogic.languageCodes("EN, ja en"))
    }

    @Test
    fun subtitleWishRules() {
        assertNull(TitleLogic.wishProblem(listOf(SubtitleWant("en"), SubtitleWant("en", forced = true))))
        assertEquals(WishProblem.TWICE, TitleLogic.wishProblem(listOf(SubtitleWant("en"), SubtitleWant("en"))))
        assertEquals(WishProblem.PICK, TitleLogic.wishProblem(listOf(SubtitleWant(""))))
        assertEquals(WishProblem.TOO_MANY, TitleLogic.wishProblem(List(11) { SubtitleWant("l$it") }))
        assertEquals("es", TitleLogic.nextLanguage(listOf(SubtitleWant("en"))))
    }

    @Test
    fun shiftGuessNeedsOneFit() {
        val leftovers =
            listOf(
                TitleLeftover(fileId = 1, reason = "episode_not_listed", guess = TitleLeftoverGuess(2, listOf(1))),
                TitleLeftover(fileId = 2, reason = "episode_not_listed", guess = TitleLeftoverGuess(2, listOf(2))),
            )
        val episodes =
            listOf(
                MatchEpisode(10, 1, 11, hasFile = true),
                MatchEpisode(11, 1, 12),
                MatchEpisode(12, 1, 13),
            )
        val guess = TitleLogic.shiftGuess(leftovers, episodes)
        assertEquals(2, guess?.fromSeason)
        assertEquals(1, guess?.toSeason)
        assertEquals(11, guess?.shift)
        assertEquals(listOf(11L, 12L), guess?.moves?.map { it.episodeId })
        assertNull(TitleLogic.shiftGuess(leftovers.take(1), episodes))
        val picked = TitleLogic.namePick(TitleLeftover(reason = "seasons_differ", guess = TitleLeftoverGuess(1, listOf(12))), episodes)
        assertEquals(listOf(11L), picked)
    }

    @Test
    fun episodeCodes() {
        assertEquals("S01E02", TitleLogic.episodeCode(1, 2))
        assertEquals("S02E10", TitleLogic.episodeCodeOf("Show/Season 2/Show.s02e10.mkv"))
    }
}
