package ch.orellbuehler.acolyte.ui.components

import android.text.format.DateUtils
import java.time.Instant

fun relativeTime(
    iso: String,
    now: Long = System.currentTimeMillis(),
): String? {
    val millis =
        try {
            Instant.parse(iso).toEpochMilli()
        } catch (e: Exception) {
            return null
        }
    return DateUtils.getRelativeTimeSpanString(millis, now, DateUtils.MINUTE_IN_MILLIS).toString()
}
