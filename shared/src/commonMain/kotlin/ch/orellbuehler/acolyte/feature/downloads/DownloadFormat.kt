package ch.orellbuehler.acolyte.feature.downloads

import kotlin.math.floor
import kotlin.math.round

object DownloadFormat {
    private val UNITS = listOf("B", "KB", "MB", "GB", "TB")

    fun size(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        var v = bytes.toDouble()
        var i = 0
        while (i < UNITS.size - 1 && round(v * 10) / 10 >= 1000) {
            v /= 1000
            i++
        }
        val text = if (i == 0 || v >= 10) round(v).toLong().toString() else oneDecimal(v)
        if (text == "1000" && i < UNITS.size - 1) return "1 ${UNITS[i + 1]}"
        return "$text ${UNITS[i]}"
    }

    fun freeSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        var v = bytes.toDouble()
        var i = 0
        while (i < UNITS.size - 1 && (if (i < 3) round(v) else round(v * 10) / 10) >= 1024) {
            v /= 1024
            i++
        }
        return if (i < 3) "${round(v).toLong()} ${UNITS[i]}" else "${oneDecimal(v, keepZero = true)} ${UNITS[i]}"
    }

    fun speed(bytes: Long): String = size(bytes) + "/s"

    fun percent(progress: Double): Int = floor(progress.coerceIn(0.0, 1.0) * 100).toInt()

    fun swarmNumber(n: Int): String = if (n >= 10000) "${round(n / 1000.0).toLong()}k" else n.coerceAtLeast(0).toString()

    private fun oneDecimal(
        v: Double,
        keepZero: Boolean = false,
    ): String {
        val tenths = round(v * 10).toLong()
        val whole = tenths / 10
        val frac = tenths % 10
        return if (frac == 0L && !keepZero) whole.toString() else "$whole.$frac"
    }
}
