package ch.orellbuehler.acolyte.feature.downloads

import ch.orellbuehler.acolyte.api.model.Decision
import kotlin.math.ceil
import kotlin.math.max
import kotlin.time.Instant

enum class DecisionFix {
    FOLDER,
    EDITION,
    VERSION,
    IMPORT,
    AGAIN,
    MATCH,
    HAND,
    SEARCH,
}

enum class ReasonKind {
    EDITION,
    COPY,
    TAKEN,
    NO_MATCH,
    REFUSED,
    PLAIN,
}

enum class NoMatchKind {
    MOVIE,
    EPISODE,
    TRACK,
}

enum class ReasonLineKind {
    READING,
    EDITION_UNKNOWN,
    COPY_IN_PLACE,
    NO_MATCH_MOVIE,
    NO_MATCH_EPISODE,
    NO_MATCH_TRACK,
    SERVER,
    WHY,
}

enum class WhyKind {
    RAN_OUT,
    CONFLICT,
    NOT_REPLACED,
    NOT_SENT,
    LINK_REFUSED,
    TAKEN,
}

data class ReasonLine(
    val kind: ReasonLineKind,
    val text: String = "",
    val why: WhyKind = WhyKind.RAN_OUT,
)

enum class DecisionHint {
    NONE,
    PATH_PAIR,
    MATCH_BY_HAND,
}

enum class DecisionStep {
    SEARCH,
    IMPORT_AGAIN,
    IMPORT_FILE,
    SET_EDITION,
    OPEN_VERSIONS,
    OPEN_RELEASES,
}

data class DecisionPlan(
    val fix: DecisionFix,
    val lead: DecisionStep,
    val beside: DecisionStep?,
    val more: List<DecisionStep>,
    val turnDown: Boolean,
    val hint: DecisionHint,
)

object DecisionRules {
    private val placingFailed = Regex("^\\s*the file (for .+? )?could not be placed\\b", RegexOption.IGNORE_CASE)
    private val copyInPlace = Regex("copy in place", RegexOption.IGNORE_CASE)
    private val editionRefusal =
        Regex("^\\s*(edition (is|does)\\b|file and release name disagree about the edition)", RegexOption.IGNORE_CASE)
    private val saveFolder = Regex("HOLYARRS_SAVE_PATH|where the Downloader says it is")
    private val noAccess =
        Regex("^\\s*HolyArrs may not (read the downloaded file|write to the folder this file goes to)", RegexOption.IGNORE_CASE)
    private val gaveUp = Regex("^\\s*the file could not be placed: something went wrong; see the log\\.?\\s*$", RegexOption.IGNORE_CASE)
    private val slowRead = Regex("^\\s*reading the download folder took too long; try again\\.?\\s*$", RegexOption.IGNORE_CASE)
    private val noMatchAgain =
        Regex(
            "^\\s*no file in the download matched (this movie|an episode|a track); match the files by hand\\.?\\s*$",
            RegexOption.IGNORE_CASE,
        )
    private val noMatch =
        Regex(
            "^(?:no file in the download matched (this movie|an episode|a track)|" +
                "nothing in the download could be matched to (an episode|a track)|" +
                "nothing could be placed[;.]|nothing could be placed$)",
            RegexOption.IGNORE_CASE,
        )
    private val refused = Regex("^HolyArrs refused .+: the Downloader reported a file outside the download folder\\.?$")

    private fun isPlacingFailed(r: String) = placingFailed.containsMatchIn(r)

    fun noSaveFolder(r: String): Boolean = !isPlacingFailed(r) && saveFolder.containsMatchIn(r)

    fun isCopyInPlace(r: String): Boolean = !isPlacingFailed(r) && copyInPlace.containsMatchIn(r)

    fun isEditionRefusal(r: String): Boolean = editionRefusal.containsMatchIn(r)

    fun canImportAgain(d: Decision): Boolean {
        val r = d.reason
        return d.downloadId > 0 &&
            (noSaveFolder(r) || noAccess.containsMatchIn(r) || gaveUp.containsMatchIn(r) || slowRead.containsMatchIn(r))
    }

    fun mayImportAgain(d: Decision): Boolean = canImportAgain(d) || (d.downloadId > 0 && noMatchAgain.containsMatchIn(d.reason))

    fun importAgainBeside(
        d: Decision,
        fix: DecisionFix,
    ): Boolean = fix != DecisionFix.AGAIN && mayImportAgain(d)

    fun noMatchKind(r: String): NoMatchKind? {
        val m = noMatch.find(r.trim()) ?: return null
        val w = (m.groupValues.getOrNull(1)?.ifEmpty { null } ?: m.groupValues.getOrNull(2).orEmpty()).lowercase()
        return when (w) {
            "an episode" -> NoMatchKind.EPISODE
            "a track" -> NoMatchKind.TRACK
            else -> NoMatchKind.MOVIE
        }
    }

    fun refusedOutside(r: String): Boolean = refused.matches(r.trim())

    fun fix(d: Decision): DecisionFix {
        val r = d.reason
        if (noSaveFolder(r)) return DecisionFix.FOLDER
        if (d.kind == "movie" && d.downloadId > 0 && isEditionRefusal(r)) return DecisionFix.EDITION
        if (d.kind == "movie" && isCopyInPlace(r)) return DecisionFix.VERSION
        if (d.kind == "movie" && d.importFile.isNotEmpty()) return DecisionFix.IMPORT
        if (canImportAgain(d)) return DecisionFix.AGAIN
        if (d.importFolder.isNotEmpty() && (noMatchKind(r) != null || refusedOutside(r))) return DecisionFix.MATCH
        if (d.why == "conflict" || d.why == "taken") return DecisionFix.HAND
        return DecisionFix.SEARCH
    }

    fun reasonKind(d: Decision): ReasonKind {
        val fix = fix(d)
        if (fix == DecisionFix.EDITION) return ReasonKind.EDITION
        if (fix == DecisionFix.VERSION) return ReasonKind.COPY
        if (d.why == "taken") return ReasonKind.TAKEN
        if (noMatchKind(d.reason) != null) return ReasonKind.NO_MATCH
        if (refusedOutside(d.reason)) return ReasonKind.REFUSED
        return ReasonKind.PLAIN
    }

    fun whyOf(d: Decision): WhyKind =
        when (d.why) {
            "conflict" -> WhyKind.CONFLICT
            "not_replaced" -> WhyKind.NOT_REPLACED
            "not_sent" -> WhyKind.NOT_SENT
            "link_refused" -> WhyKind.LINK_REFUSED
            "taken" -> WhyKind.TAKEN
            else -> WhyKind.RAN_OUT
        }

    fun reasonLine(d: Decision): ReasonLine {
        val why = whyOf(d)
        if (d.reading) return ReasonLine(ReasonLineKind.READING, why = why)
        val kind = reasonKind(d)
        if (kind == ReasonKind.EDITION) return ReasonLine(ReasonLineKind.EDITION_UNKNOWN, why = why)
        if (kind == ReasonKind.COPY) return ReasonLine(ReasonLineKind.COPY_IN_PLACE, why = why)
        if (kind == ReasonKind.TAKEN && d.reason.isEmpty()) return ReasonLine(ReasonLineKind.WHY, why = why)
        if (d.why == "link_refused" &&
            d.reason.isNotEmpty()
        ) {
            return ReasonLine(ReasonLineKind.SERVER, d.reason.removePrefix("not sent: "), why)
        }
        if (kind == ReasonKind.NO_MATCH) {
            val line =
                when (noMatchKind(d.reason)) {
                    NoMatchKind.EPISODE -> ReasonLineKind.NO_MATCH_EPISODE
                    NoMatchKind.TRACK -> ReasonLineKind.NO_MATCH_TRACK
                    else -> ReasonLineKind.NO_MATCH_MOVIE
                }
            return ReasonLine(line, why = why)
        }
        if (d.reason.isNotBlank()) return ReasonLine(ReasonLineKind.SERVER, plainReason(d.reason), why)
        return ReasonLine(ReasonLineKind.WHY, why = why)
    }

    fun plainReason(reason: String): String {
        val r = reason.trim()
        val cut = Regex("^(the file (for .+? )?could not be placed)\\s*:", RegexOption.IGNORE_CASE).find(r)
        val lead = cut?.groupValues?.get(1) ?: return r
        return lead.replaceFirstChar { it.uppercase() } + "."
    }

    fun retryMinutes(
        retryAt: String,
        now: Instant,
    ): Int? {
        val at =
            try {
                Instant.parse(retryAt)
            } catch (e: IllegalArgumentException) {
                return null
            }
        val millis = (at - now).inWholeMilliseconds
        return max(1, ceil(millis / 60_000.0).toInt())
    }

    fun packImportAgain(items: List<Decision>): Boolean {
        val a = items.firstOrNull() ?: return false
        if (!canImportAgain(a)) return false
        val r = a.reason.trim()
        return items.all { it.downloadId == a.downloadId && it.reason.trim() == r }
    }

    fun plan(d: Decision): DecisionPlan {
        val fix = fix(d)
        val again = mayImportAgain(d)
        val lead =
            when (fix) {
                DecisionFix.EDITION -> DecisionStep.SET_EDITION
                DecisionFix.VERSION -> DecisionStep.OPEN_VERSIONS
                DecisionFix.IMPORT -> DecisionStep.IMPORT_FILE
                DecisionFix.AGAIN -> DecisionStep.IMPORT_AGAIN
                DecisionFix.HAND -> DecisionStep.OPEN_RELEASES
                DecisionFix.SEARCH -> DecisionStep.SEARCH
                DecisionFix.FOLDER, DecisionFix.MATCH -> if (again) DecisionStep.IMPORT_AGAIN else DecisionStep.SEARCH
            }.let {
                if (d.kind == "artist" &&
                    (it == DecisionStep.OPEN_RELEASES || it == DecisionStep.OPEN_VERSIONS)
                ) {
                    DecisionStep.SEARCH
                } else {
                    it
                }
            }
        val beside = if (lead != DecisionStep.IMPORT_AGAIN && again) DecisionStep.IMPORT_AGAIN else null
        val more = mutableListOf<DecisionStep>()
        if (d.importFile.isNotEmpty() && d.kind == "movie" && lead != DecisionStep.IMPORT_FILE) more += DecisionStep.IMPORT_FILE
        if (lead != DecisionStep.SEARCH) more += DecisionStep.SEARCH
        if (lead != DecisionStep.OPEN_RELEASES && d.kind != "artist") more += DecisionStep.OPEN_RELEASES
        if (d.kind == "movie" && lead != DecisionStep.OPEN_VERSIONS) more += DecisionStep.OPEN_VERSIONS
        val hint =
            when (fix) {
                DecisionFix.FOLDER -> DecisionHint.PATH_PAIR
                DecisionFix.MATCH -> DecisionHint.MATCH_BY_HAND
                else -> DecisionHint.NONE
            }
        return DecisionPlan(fix, lead, beside, more, d.releaseId > 0, hint)
    }
}

data class DecisionGroup(
    val key: String,
    val lead: Decision,
    val items: List<Decision>,
    val pack: Boolean,
) {
    val packImportAgain: Boolean get() = pack && DecisionRules.packImportAgain(items)
}

fun groupDecisions(items: List<Decision>): List<DecisionGroup> {
    val out = mutableListOf<DecisionGroup>()
    val byPack = linkedMapOf<Long, MutableList<Decision>>()
    val order = mutableListOf<Any>()
    for (d in items) {
        val packed = (d.kind == "show" || d.kind == "artist") && d.downloadId > 0 && d.packSlotIds.size > 1
        if (!packed) {
            order += DecisionGroup("slot-${d.slotId}", d, listOf(d), false)
            continue
        }
        val list = byPack[d.downloadId]
        if (list != null) {
            list += d
        } else {
            byPack[d.downloadId] = mutableListOf(d)
            order += d.downloadId
        }
    }
    for (o in order) {
        if (o is DecisionGroup) {
            out += o
        } else {
            val list = byPack.getValue(o as Long).sortedWith(compareBy({ it.episode?.season ?: 0 }, { it.episode?.episode ?: 0 }))
            out += DecisionGroup("pack-$o", list.first(), list, true)
        }
    }
    return out
}
