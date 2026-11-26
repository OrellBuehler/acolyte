package ch.orellbuehler.acolyte.tv.popular

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.feature.discover.RailDayEntry
import ch.orellbuehler.acolyte.feature.discover.RailEntry
import ch.orellbuehler.acolyte.feature.discover.WhatKind
import ch.orellbuehler.acolyte.tv.R
import ch.orellbuehler.acolyte.tv.components.tvMonthDay
import ch.orellbuehler.acolyte.tv.components.tvShortDay
import ch.orellbuehler.acolyte.tv.components.tvWeekday

@Composable
fun tvDayTitle(day: RailDayEntry): String {
    val word =
        when (day.offset) {
            0 -> stringResource(R.string.tv_discover_day_today)
            -1 -> stringResource(R.string.tv_discover_day_yesterday)
            1 -> stringResource(R.string.tv_discover_day_tomorrow)
            else -> tvWeekday(day.day)
        }
    return "$word · ${if (day.offset in -1..1) tvShortDay(day.day) else tvMonthDay(day.day)}"
}

@Composable
fun tvRailWhat(entry: RailEntry): String {
    val base =
        when (entry.what) {
            WhatKind.ON_SERVICE -> stringResource(R.string.tv_discover_on_service, entry.text)
            WhatKind.OUT -> stringResource(R.string.tv_discover_what_out)
            WhatKind.COMING -> stringResource(R.string.tv_discover_what_coming)
            WhatKind.OTHER -> entry.text
        }
    return if (entry.alsoComing) "$base · ${stringResource(R.string.tv_discover_what_coming)}" else base
}
