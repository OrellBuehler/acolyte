package ch.orellbuehler.acolyte.feature.acquisition

enum class SettingsNamingWordSet {
    MOVIE,
    SEASON,
    EPISODE,
    MUSIC,
    NONE,
}

enum class SettingsNamingText(
    val wire: String,
    val wordSet: SettingsNamingWordSet,
) {
    FILE_PATTERN("file_pattern", SettingsNamingWordSet.MOVIE),
    FOLDER_PATTERN("folder_pattern", SettingsNamingWordSet.MOVIE),
    SHOW_FOLDER_PATTERN("show_folder_pattern", SettingsNamingWordSet.MOVIE),
    SEASON_FOLDER_PATTERN("season_folder_pattern", SettingsNamingWordSet.SEASON),
    SPECIALS_FOLDER("specials_folder", SettingsNamingWordSet.NONE),
    EPISODE_PATTERN("episode_pattern", SettingsNamingWordSet.EPISODE),
    DAILY_PATTERN("daily_pattern", SettingsNamingWordSet.EPISODE),
    ANIME_PATTERN("anime_pattern", SettingsNamingWordSet.EPISODE),
    ARTIST_FOLDER_PATTERN("artist_folder_pattern", SettingsNamingWordSet.MUSIC),
    ALBUM_FOLDER_PATTERN("album_folder_pattern", SettingsNamingWordSet.MUSIC),
    TRACK_PATTERN("track_pattern", SettingsNamingWordSet.MUSIC),
    MULTI_DISC_TRACK_PATTERN("multi_disc_track_pattern", SettingsNamingWordSet.MUSIC),
    DISC_FOLDER_PATTERN("disc_folder_pattern", SettingsNamingWordSet.MUSIC),
    ;

    val insertable: Boolean get() = wordSet != SettingsNamingWordSet.NONE

    companion object {
        fun of(wire: String): SettingsNamingText? = entries.firstOrNull { it.wire == wire }
    }
}

enum class SettingsNamingSwitch(
    val wire: String,
) {
    UNKEEP_MOVIES("unkeep_deleted_movies"),
    UNKEEP_EPISODES("unkeep_deleted_episodes"),
    SKIP_FREE_CHECK("skip_free_space_check"),
    GROUP_BY_GENRE("group_by_genre"),
    GROUP_BY_LETTER("group_by_letter"),
    SHOW_GROUP_BY_LETTER("show_group_by_letter"),
    SEASON_FOLDERS("season_folders"),
    DISC_FOLDERS("disc_folders"),
    READ_TAGS("music_read_tags"),
    RENAME_MOVIES("rename_on_import"),
    RENAME_SHOWS("show_rename_on_import"),
    RENAME_MUSIC("music_rename_on_import"),
    ;

    companion object {
        fun of(wire: String): SettingsNamingSwitch? = entries.firstOrNull { it.wire == wire }
    }
}

private val COLON_OPTIONS = listOf("smart", "dash", "space_dash", "space_dash_space", "remove")
private val ILLEGAL_OPTIONS = listOf("replace", "remove")

enum class SettingsNamingChoice(
    val wire: String,
    val options: List<String>,
) {
    COLON("colon", COLON_OPTIONS),
    ILLEGAL("illegal", ILLEGAL_OPTIONS),
    SHOW_COLON("show_colon", COLON_OPTIONS),
    SHOW_ILLEGAL("show_illegal", ILLEGAL_OPTIONS),
    MUSIC_COLON("music_colon", COLON_OPTIONS),
    MUSIC_ILLEGAL("music_illegal", ILLEGAL_OPTIONS),
    MULTI_EPISODE("multi_episode_style", listOf("range", "short_range", "each", "each_dash")),
    AIR_DATE("air_date_style", listOf("dash", "dot", "space")),
    ABSOLUTE_DIGITS("absolute_digits", listOf("2", "3", "4")),
    TITLE_LANGUAGE("title_language", listOf("") + SETTINGS_NAMING_LANGUAGES.map { it.code }),
    ;

    companion object {
        fun of(wire: String): SettingsNamingChoice? = entries.firstOrNull { it.wire == wire }
    }
}

enum class SettingsNamingNumber(
    val wire: String,
    val min: Int,
    val max: Int,
) {
    HOLDING_DAYS("holding_days", 0, 3650),
    MIN_FREE_MB("min_free_mb", 100, 1048576),
    ADOPT_SCAN_LIMIT("adopt_scan_limit", 10, 100000),
    MUSIC_ADOPT_SCAN_LIMIT("music_adopt_scan_limit", 10, 100000),
    ;

    companion object {
        fun of(wire: String): SettingsNamingNumber? = entries.firstOrNull { it.wire == wire }
    }
}

enum class SettingsNamingNumberError {
    DAYS,
    DAYS_MAX,
    FREE,
    FREE_MAX,
    RANGE,
}

fun settingsNamingNumberError(
    field: SettingsNamingNumber,
    text: String,
): SettingsNamingNumberError? {
    val value = text.trim().toLongOrNull()
    val low =
        when (field) {
            SettingsNamingNumber.HOLDING_DAYS -> SettingsNamingNumberError.DAYS
            SettingsNamingNumber.MIN_FREE_MB -> SettingsNamingNumberError.FREE
            else -> SettingsNamingNumberError.RANGE
        }
    return when {
        value == null || value < field.min -> low
        value <= field.max -> null
        field == SettingsNamingNumber.HOLDING_DAYS -> SettingsNamingNumberError.DAYS_MAX
        field == SettingsNamingNumber.MIN_FREE_MB -> SettingsNamingNumberError.FREE_MAX
        else -> SettingsNamingNumberError.RANGE
    }
}

fun settingsNamingParseNumber(
    field: SettingsNamingNumber,
    text: String,
): Int? = if (settingsNamingNumberError(field, text) == null) text.trim().toInt() else null

enum class SettingsNamingStyle(
    val wire: String,
    internal val folder: String,
    internal val file: String,
    internal val show: String,
) {
    PLEX(
        "plex",
        "{title} ({year}) {tmdb-{tmdb}}",
        "{title} ({year}) {tmdb-{tmdb}} - {edition-{edition}}$STYLE_TAIL",
        "{title} ({year}) {tvdb-{tvdb}}",
    ),
    JELLYFIN(
        "jellyfin",
        "{title} ({year}) [tmdbid-{tmdb}]",
        "{title} ({year}) [tmdbid-{tmdb}] - {{edition}}$STYLE_TAIL",
        "{title} ({year}) [tvdbid-{tvdb}]",
    ),
    EMBY(
        "emby",
        "{title} ({year}) [tmdb-{tmdb}]",
        "{title} ({year}) [tmdb-{tmdb}] - {edition-{edition}}$STYLE_TAIL",
        "{title} ({year}) [tvdb-{tvdb}]",
    ),
    ;

    internal val episode: String get() = "{title} ({year}) - {episode} - {episode_title}$STYLE_TAIL"

    companion object {
        fun of(wire: String): SettingsNamingStyle? = entries.firstOrNull { it.wire == wire }
    }
}

data class SettingsNamingValue(
    val wire: String,
    val value: String,
)

data class SettingsNamingWord(
    val key: String,
    val about: String,
)

data class SettingsNamingExampleRow(
    val key: String,
    val path: String,
)

data class SettingsNamingLanguage(
    val code: String,
    val name: String,
)

data class SettingsNamingInsert(
    val text: String,
    val cursor: Int,
)

enum class SettingsNamingCleanNote {
    REMOVING,
    NOTHING,
}

fun settingsNamingInsert(
    text: String,
    start: Int,
    end: Int,
    word: String,
): SettingsNamingInsert {
    val from = start.coerceIn(0, text.length)
    val to = end.coerceIn(from, text.length)
    val glue = if (from == text.length && text.isNotEmpty() && !text.endsWith(" ")) " " else ""
    val inserted = "$glue{$word}"
    return SettingsNamingInsert(text.substring(0, from) + inserted + text.substring(to), from + inserted.length)
}

private val SHOW_WORDS = setOf("season", "season_padded", "episode", "episode_title", "air_date", "absolute")
private val MUSIC_WORDS =
    setOf("artist", "artist_sort", "album", "album_type", "disc", "track", "track_title", "track_artist", "format")
private val MUSIC_SHARED_WORDS = setOf("year", "edition")
private val SEASON_WORDS = setOf("season", "season_padded")

internal fun settingsNamingWords(
    words: Map<String, String>,
    set: SettingsNamingWordSet,
    query: String,
): List<SettingsNamingWord> {
    val wanted = query.trim().lowercase()
    return words.entries
        .filter { (key, _) ->
            when (set) {
                SettingsNamingWordSet.MOVIE -> key !in SHOW_WORDS && key !in MUSIC_WORDS
                SettingsNamingWordSet.SEASON -> key in SEASON_WORDS
                SettingsNamingWordSet.EPISODE -> key !in MUSIC_WORDS
                SettingsNamingWordSet.MUSIC -> key in MUSIC_WORDS || key in MUSIC_SHARED_WORDS
                SettingsNamingWordSet.NONE -> false
            }
        }.filter { (key, about) -> wanted.isEmpty() || key.lowercase().contains(wanted) || about.lowercase().contains(wanted) }
        .sortedBy { it.key }
        .map { SettingsNamingWord(it.key, it.value) }
}

internal fun settingsNamingRows(
    examples: Map<String, String>,
    keys: List<String>,
): List<SettingsNamingExampleRow> =
    keys.mapNotNull { key ->
        examples[key]?.takeIf { it.isNotEmpty() }?.let { SettingsNamingExampleRow(key, it) }
    }

internal val SETTINGS_NAMING_EPISODE_KEYS = listOf("show_folder", "episode", "multi", "special", "daily", "anime")
internal val SETTINGS_NAMING_MUSIC_KEYS = listOf("artist_folder", "album_folder", "track", "multi_disc", "various")

private const val STYLE_TAIL = " [{quality_full}][{audio} {audio_channels}][{hdr_type}][{video_codec}]-{group}"

internal val SETTINGS_NAMING_LANGUAGES: List<SettingsNamingLanguage> =
    listOf(
        "es" to "Español",
        "pt-br" to "Português (Brasil)",
        "pt-pt" to "Português (Portugal)",
        "fr" to "Français",
        "de" to "Deutsch",
        "it" to "Italiano",
        "nl" to "Nederlands",
        "sv" to "Svenska",
        "nb" to "Norsk bokmål",
        "da" to "Dansk",
        "fi" to "Suomi",
        "ru" to "Русский",
        "uk" to "Українська",
        "pl" to "Polski",
        "cs" to "Čeština",
        "hu" to "Magyar",
        "ro" to "Română",
        "el" to "Ελληνικά",
        "sk" to "Slovenčina",
        "bg" to "Български",
        "sr" to "Српски",
        "hr" to "Hrvatski",
        "ca" to "Català",
        "lt" to "Lietuvių",
        "lv" to "Latviešu",
        "et" to "Eesti",
        "is" to "Íslenska",
        "zh-cn" to "简体中文",
        "zh-tw" to "繁體中文",
        "ja" to "日本語",
        "ko" to "한국어",
        "hi" to "हिन्दी",
        "bn" to "বাংলা",
        "ta" to "தமிழ்",
        "id" to "Bahasa Indonesia",
        "ms" to "Bahasa Melayu",
        "fil" to "Filipino",
        "vi" to "Tiếng Việt",
        "th" to "ไทย",
        "tr" to "Türkçe",
        "he" to "עברית",
        "fa" to "فارسی",
        "ar" to "العربية",
    ).map { SettingsNamingLanguage(it.first, it.second) }
