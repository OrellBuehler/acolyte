package ch.orellbuehler.acolyte.feature.downloads

import java.util.TimeZone

internal actual fun localOffsetSeconds(epochSeconds: Long): Int = TimeZone.getDefault().getOffset(epochSeconds * 1000) / 1000
