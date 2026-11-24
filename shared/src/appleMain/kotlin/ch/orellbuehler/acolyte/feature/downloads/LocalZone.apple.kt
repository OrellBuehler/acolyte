package ch.orellbuehler.acolyte.feature.downloads

import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localTimeZone

internal actual fun localOffsetSeconds(epochSeconds: Long): Int =
    NSTimeZone.localTimeZone.secondsFromGMTForDate(NSDate.dateWithTimeIntervalSince1970(epochSeconds.toDouble())).toInt()
