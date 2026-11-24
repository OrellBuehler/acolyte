package ch.orellbuehler.acolyte.nav

import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.Part

enum class LibraryTool(
    internal val need: Need,
    internal val part: Part,
    internal val touchesFiles: Boolean,
) {
    STATS(Need.MANAGE_LIBRARY, Part.MEDIA, false),
    DELETED(Need.MANAGE_LIBRARY, Part.MEDIA, true),
    COPIES(Need.MANAGE_LIBRARY, Part.MOVIES, false),
    HELD(Need.MANAGE_LIBRARY, Part.MEDIA, false),
    TURNED_DOWN(Need.MANAGE_LIBRARY, Part.MEDIA, false),
    MISSING_SUBTITLES(Need.MANAGE_LIBRARY, Part.SUBTITLES, true),
    RENAME(Need.MANAGE_LIBRARY, Part.MEDIA, true),
    IMPORT(Need.MANAGE_LIBRARY, Part.MEDIA, true),
    IMPORT_FILES(Need.SETTINGS, Part.MEDIA, true),
    LIBRARY_FOLDERS(Need.SETTINGS, Part.CORE, false),
}
