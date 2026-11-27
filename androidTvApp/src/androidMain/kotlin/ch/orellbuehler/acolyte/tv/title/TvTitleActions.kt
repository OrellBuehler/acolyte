package ch.orellbuehler.acolyte.tv.title

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.ui.graphics.vector.ImageVector
import ch.orellbuehler.acolyte.feature.title.AskOffer
import ch.orellbuehler.acolyte.feature.title.TitleState
import ch.orellbuehler.acolyte.tv.R

enum class TvTitleActionKind {
    ASK,
    TRAILER,
    ASK_4K,
    FOLLOW,
}

data class TvTitleAction(
    val kind: TvTitleActionKind,
    @StringRes val label: Int,
    val icon: ImageVector?,
    val enabled: Boolean,
)

fun tvTitleActions(
    state: TitleState,
    trailerLoading: Boolean,
): List<TvTitleAction> {
    val actions = mutableListOf<TvTitleAction>()
    state.askOffer?.let { offer ->
        actions +=
            TvTitleAction(
                TvTitleActionKind.ASK,
                askLabel(offer),
                if (offer == AskOffer.ASKED) Icons.Rounded.Check else Icons.Rounded.Add,
                offer != AskOffer.ASKED,
            )
    }
    if (state.trailer != null) {
        actions += TvTitleAction(TvTitleActionKind.TRAILER, R.string.tv_title_trailer, Icons.Rounded.PlayCircle, !trailerLoading)
    }
    state.fourKOffer?.let { offer ->
        val label = if (offer == AskOffer.ASKED) R.string.tv_title_requested_4k else R.string.tv_title_request_4k
        actions += TvTitleAction(TvTitleActionKind.ASK_4K, label, null, offer != AskOffer.ASKED)
    }
    if (state.access.followOn && state.hasTmdb) {
        actions +=
            TvTitleAction(
                TvTitleActionKind.FOLLOW,
                if (state.following) R.string.tv_title_following else R.string.tv_title_follow,
                if (state.following) Icons.Rounded.NotificationsActive else Icons.Rounded.Notifications,
                !state.followBusy,
            )
    }
    return actions
}

@StringRes
private fun askLabel(offer: AskOffer): Int =
    when (offer) {
        AskOffer.ASK, AskOffer.SEASONS -> R.string.tv_title_request
        AskOffer.ASK_TOO -> R.string.tv_title_request_too
        AskOffer.ASKED -> R.string.tv_title_requested
    }
