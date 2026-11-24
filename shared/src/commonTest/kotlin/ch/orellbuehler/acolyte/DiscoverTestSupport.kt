package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.model.Me
import ch.orellbuehler.acolyte.api.model.PartView
import ch.orellbuehler.acolyte.api.model.Parts
import ch.orellbuehler.acolyte.feature.discover.DiscoverPrefs
import ch.orellbuehler.acolyte.feature.discover.OwnedLibrary
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.AccountSnapshot
import ch.orellbuehler.acolyte.session.MemoryStore
import ch.orellbuehler.acolyte.session.SignInMethod

internal fun discoverAccount(
    role: String = "member",
    perms: List<String> = listOf("ask"),
    off: List<String> = emptyList(),
    maxRating: String = "",
    displayName: String = "",
): Account {
    val parts = listOf("movies", "shows", "music", "subtitles", "wishlist", "sources").map { PartView(part = it, on = it !in off) }
    val me = Me(id = 2, name = "sam", role = role, perms = perms, maxRating = maxRating)
    val account = Account.from(BASE, SignInMethod.PASSWORD, AccountSnapshot(me, Parts(parts)))
    return if (displayName.isEmpty()) account else account.copy(displayName = displayName)
}

internal val MANAGER_PERMS = listOf("ask", "approve", "manage_library", "settings")

internal fun owned(api: Api): OwnedLibrary = OwnedLibrary(api)

internal fun prefs(store: MemoryStore = MemoryStore()): DiscoverPrefs = DiscoverPrefs(store) { "test" }

internal fun item(
    tmdb: String,
    kind: String = "movie",
    name: String = "Title $tmdb",
    extra: String = "",
    inLibrary: Boolean = false,
): String =
    """{"tmdb_id":"$tmdb","kind":"$kind","name":"$name","year":2024,"poster":"https://image.tmdb.org/t/p/w342/p$tmdb.jpg",""" +
        """"backdrop":"https://image.tmdb.org/t/p/w1280/b$tmdb.jpg","genres":["Drama"],"in_library":$inLibrary,"ask":null$extra}"""

internal fun row(
    id: String,
    type: String,
    items: List<String>,
    title: String = "Row $id",
): String = """{"id":"$id","type":"$type","title":"$title","items":[${items.joinToString(",")}]}"""
