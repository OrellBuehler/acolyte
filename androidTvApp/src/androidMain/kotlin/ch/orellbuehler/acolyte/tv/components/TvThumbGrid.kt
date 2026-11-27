package ch.orellbuehler.acolyte.tv.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ch.orellbuehler.acolyte.tv.theme.TvMetrics

fun <T> LazyListScope.tvThumbRows(
    items: List<T>,
    key: (T) -> Any,
    content: @Composable (T, Modifier) -> Unit,
) {
    items(items.chunked(2), key = { row -> row.joinToString("|") { key(it).toString() } }) { row ->
        Row(
            Modifier.tvRowBounds().padding(horizontal = TvMetrics.marginX, vertical = TvMetrics.textGap / 2),
            horizontalArrangement = Arrangement.spacedBy(TvMetrics.gutter),
        ) {
            row.forEach { content(it, Modifier.weight(1f)) }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}
