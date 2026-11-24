package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.deviceLanguage
import ch.orellbuehler.acolyte.api.platformEngine
import ch.orellbuehler.acolyte.feature.health.HealthPresenter
import ch.orellbuehler.acolyte.feature.library.LibraryCopiesPresenter
import ch.orellbuehler.acolyte.feature.library.LibraryDeletedPresenter
import ch.orellbuehler.acolyte.feature.library.LibraryFilesPresenter
import ch.orellbuehler.acolyte.feature.library.LibraryFoldersPresenter
import ch.orellbuehler.acolyte.feature.library.LibraryImportPresenter
import ch.orellbuehler.acolyte.feature.library.LibraryKind
import ch.orellbuehler.acolyte.feature.library.LibraryPrefs
import ch.orellbuehler.acolyte.feature.library.LibraryPresenter
import ch.orellbuehler.acolyte.feature.library.LibraryReleasesPresenter
import ch.orellbuehler.acolyte.feature.library.LibraryRenamePresenter
import ch.orellbuehler.acolyte.feature.library.LibraryStatsPresenter
import ch.orellbuehler.acolyte.feature.library.LibrarySubtitlesPresenter
import ch.orellbuehler.acolyte.feature.title.TitlePresenter
import ch.orellbuehler.acolyte.image.Images
import ch.orellbuehler.acolyte.nav.LibraryTool
import ch.orellbuehler.acolyte.session.SecureStore
import ch.orellbuehler.acolyte.session.SessionManager
import ch.orellbuehler.acolyte.session.SignInFlow
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AcolyteSdk internal constructor(
    store: SecureStore,
    appVersion: String,
    engineFactory: () -> HttpClientEngine,
    scope: CoroutineScope,
) {
    constructor(store: SecureStore, appVersion: String) : this(
        store,
        appVersion,
        ::platformEngine,
        CoroutineScope(SupervisorJob() + Dispatchers.Default),
    )

    val session: SessionManager = SessionManager(store, engineFactory, appVersion, scope)

    val images: Images = Images { session.client }

    fun signIn(): SignInFlow = SignInFlow(session)

    fun health(): HealthPresenter = HealthPresenter(session.api)

    private val libraryPrefs = LibraryPrefs(store)

    fun library(kind: LibraryKind?): LibraryPresenter = LibraryPresenter(session.api, { session.account }, libraryPrefs, kind)

    fun libraryTools(): List<LibraryTool> = session.account?.libraryTools.orEmpty()

    fun libraryStats(): LibraryStatsPresenter = LibraryStatsPresenter(session.api) { session.account }

    fun libraryCopies(): LibraryCopiesPresenter = LibraryCopiesPresenter(session.api)

    fun libraryDeleted(titleId: Long): LibraryDeletedPresenter = LibraryDeletedPresenter(session.api, { session.account }, titleId)

    fun libraryReleases(turnedDown: Boolean): LibraryReleasesPresenter = LibraryReleasesPresenter(session.api, turnedDown)

    fun librarySubtitles(): LibrarySubtitlesPresenter = LibrarySubtitlesPresenter(session.api)

    fun libraryRename(): LibraryRenamePresenter = LibraryRenamePresenter(session.api) { session.account }

    fun libraryImport(): LibraryImportPresenter =
        LibraryImportPresenter(session.api) { session.account?.libraryTools?.contains(LibraryTool.LIBRARY_FOLDERS) == true }

    fun libraryFiles(path: String): LibraryFilesPresenter = LibraryFilesPresenter(session.api, { session.account }, path)

    fun libraryFolders(): LibraryFoldersPresenter =
        LibraryFoldersPresenter(
            session.api,
            { session.account },
            { password, code -> session.reauth(password, code) },
            { session.refresh() },
        )

    fun title(
        kind: String,
        tmdb: String,
        libraryId: Long,
        openLibrary: Boolean,
    ): TitlePresenter = TitlePresenter(session.api, kind, tmdb, libraryId, openLibrary, session.account, deviceLanguage())
}
