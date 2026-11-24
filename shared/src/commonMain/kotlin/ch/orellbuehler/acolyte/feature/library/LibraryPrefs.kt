package ch.orellbuehler.acolyte.feature.library

import ch.orellbuehler.acolyte.api.ApiJson
import ch.orellbuehler.acolyte.session.SecureStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer

@Serializable
data class SavedFilter(
    val name: String,
    val filter: LibraryFilter,
)

internal class LibraryPrefs(
    private val store: SecureStore,
) {
    private val serializer = ListSerializer(SavedFilter.serializer())

    private fun filtersKey(
        owner: String,
        kind: LibraryKind,
    ) = "library.filters.$owner.${kind.wire}"

    fun filters(
        owner: String,
        kind: LibraryKind,
    ): List<SavedFilter> =
        store.read(filtersKey(owner, kind))?.let {
            try {
                ApiJson.lenient.decodeFromString(serializer, it)
            } catch (e: SerializationException) {
                null
            } catch (e: IllegalArgumentException) {
                null
            }
        } ?: emptyList()

    fun saveFilters(
        owner: String,
        kind: LibraryKind,
        filters: List<SavedFilter>,
    ) {
        store.write(filtersKey(owner, kind), ApiJson.lenient.encodeToString(serializer, filters.takeLast(MAX_FILTERS)))
    }

    fun lastKind(owner: String): LibraryKind? = store.read("library.kind.$owner")?.let { LibraryKind.of(it) }

    fun saveLastKind(
        owner: String,
        kind: LibraryKind,
    ) {
        store.write("library.kind.$owner", kind.wire)
    }

    companion object {
        const val MAX_FILTERS = 20
        const val MAX_NAME = 60
    }
}
