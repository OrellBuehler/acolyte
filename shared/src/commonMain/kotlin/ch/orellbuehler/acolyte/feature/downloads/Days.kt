package ch.orellbuehler.acolyte.feature.downloads

import kotlin.time.Instant

internal expect fun localOffsetSeconds(epochSeconds: Long): Int

data class Day(
    val year: Int,
    val month: Int,
    val day: Int,
) : Comparable<Day> {
    val epochDay: Long get() = Days.epochDay(year, month, day)

    val key: String get() = "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

    val weekday: Int get() = Days.weekday(epochDay)

    fun plusDays(n: Int): Day = Days.fromEpochDay(epochDay + n)

    override fun compareTo(other: Day): Int = epochDay.compareTo(other.epochDay)

    companion object {
        fun parse(text: String): Day? {
            val m = Regex("^(\\d{4})-(\\d{2})-(\\d{2})").find(text) ?: return null
            val (y, mo, d) = m.destructured
            val day = Day(y.toInt(), mo.toInt(), d.toInt())
            return if (day.month in 1..12 && day.day in 1..Days.monthLength(day.year, day.month)) day else null
        }
    }
}

data class Month(
    val year: Int,
    val month: Int,
) {
    val first: Day get() = Day(year, month, 1)

    val length: Int get() = Days.monthLength(year, month)

    val last: Day get() = Day(year, month, length)

    fun plus(n: Int): Month {
        val index = year * 12 + (month - 1) + n
        return Month(IntMath.floorDiv(index, 12), IntMath.floorMod(index, 12) + 1)
    }

    val key: String get() = "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}"
}

private object IntMath {
    fun floorDiv(
        a: Int,
        b: Int,
    ): Int {
        val q = a / b
        return if ((a % b != 0) && ((a < 0) != (b < 0))) q - 1 else q
    }

    fun floorMod(
        a: Int,
        b: Int,
    ): Int = a - floorDiv(a, b) * b
}

object Days {
    fun epochDay(
        year: Int,
        month: Int,
        day: Int,
    ): Long {
        val y = (if (month <= 2) year - 1 else year).toLong()
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val mp = (month + 9) % 12
        val doy = (153 * mp + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }

    fun fromEpochDay(days: Long): Day {
        val z = days + 719468
        val era = (if (z >= 0) z else z - 146096) / 146097
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = (doy - (153 * mp + 2) / 5 + 1).toInt()
        val m = (if (mp < 10) mp + 3 else mp - 9).toInt()
        val y = (yoe + era * 400 + if (m <= 2) 1 else 0).toInt()
        return Day(y, m, d)
    }

    fun weekday(epochDay: Long): Int = (((epochDay + 3) % 7 + 7) % 7).toInt() + 1

    fun monthLength(
        year: Int,
        month: Int,
    ): Int =
        when (month) {
            2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }

    fun localDay(
        at: Instant,
        offset: (Long) -> Int = ::localOffsetSeconds,
    ): Day {
        val seconds = at.epochSeconds
        val local = seconds + offset(seconds)
        return fromEpochDay(kotlin.math.floor(local / 86400.0).toLong())
    }

    fun localDayOf(
        iso: String,
        offset: (Long) -> Int = ::localOffsetSeconds,
    ): Day? = parseInstant(iso)?.let { localDay(it, offset) }

    fun localMinutes(
        at: Instant,
        offset: (Long) -> Int = ::localOffsetSeconds,
    ): Int {
        val seconds = at.epochSeconds + offset(at.epochSeconds)
        return (((seconds % 86400) + 86400) % 86400 / 60).toInt()
    }

    fun parseInstant(iso: String): Instant? =
        try {
            if (iso.isBlank()) null else Instant.parse(iso)
        } catch (e: IllegalArgumentException) {
            null
        }

    fun today(
        now: Instant,
        offset: (Long) -> Int = ::localOffsetSeconds,
    ): Day = localDay(now, offset)

    fun weekStart(day: Day): Day = day.plusDays(-(day.weekday - 1))
}
