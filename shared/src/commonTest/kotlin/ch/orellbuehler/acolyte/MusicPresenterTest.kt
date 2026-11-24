package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.music.MusicAddOutcome
import ch.orellbuehler.acolyte.feature.music.MusicAddPresenter
import ch.orellbuehler.acolyte.feature.music.MusicAlbumCta
import ch.orellbuehler.acolyte.feature.music.MusicAlbumPresenter
import ch.orellbuehler.acolyte.feature.music.MusicArtistPresenter
import ch.orellbuehler.acolyte.feature.music.MusicBrowsePresenter
import ch.orellbuehler.acolyte.feature.music.MusicCardAction
import ch.orellbuehler.acolyte.feature.music.MusicHave
import ch.orellbuehler.acolyte.feature.music.MusicLeftoversPresenter
import ch.orellbuehler.acolyte.feature.music.MusicLibraryAlbumPresenter
import ch.orellbuehler.acolyte.feature.music.MusicLibraryArtistPresenter
import ch.orellbuehler.acolyte.feature.music.MusicMain
import ch.orellbuehler.acolyte.feature.music.MusicNewPresenter
import ch.orellbuehler.acolyte.feature.music.MusicNoteKind
import ch.orellbuehler.acolyte.feature.music.MusicRowsPresenter
import ch.orellbuehler.acolyte.feature.music.MusicSearchPresenter
import ch.orellbuehler.acolyte.feature.music.MusicSettingsPresenter
import ch.orellbuehler.acolyte.feature.music.MusicShelfKind
import ch.orellbuehler.acolyte.feature.music.MusicUnmatchedPresenter
import ch.orellbuehler.acolyte.session.Role
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val TODAY = "2026-11-24"
private const val MBID = "5b11f4ce-a62d-471e-81fc-a69a8278c7da"
private const val ALBUM = "11111111-1111-1111-1111-111111111111"

private val ARTIST_JSON =
    """{"mbid":"$MBID","name":"Nirvana","type":"Group","country":"US","status":"ended","genres":["grunge","rock"],"image":"/api/v1/music-trending/art/artist/$MBID",
    "have":0,"total":3,"bio":{"title":"Nirvana","extract":"A band.","url":"https://w.test/n","license":"CC BY-SA 4.0","license_url":"https://cc.test","attribution":"Wikipedia"},
    "albums":[
    {"mbid":"$ALBUM","name":"Nevermind","type":"album","secondary_types":[],"release_date":"1991-09-24","shelf":"albums","image":"/api/v1/music-trending/art/album/$ALBUM","state":"complete","part_id":9},
    {"mbid":"22222222-2222-2222-2222-222222222222","name":"Bleach","type":"album","release_date":"1989-06-15","shelf":"albums","image":"/img/b"},
    {"mbid":"33333333-3333-3333-3333-333333333333","name":"Live","type":"album","secondary_types":["live"],"release_date":"1994-11-01","shelf":"live","image":"/img/l"},
    {"mbid":"44444444-4444-4444-4444-444444444444","name":"Next","type":"album","release_date":"2027-02-01","shelf":"albums","upcoming":true,"image":"/img/n"}],
    "top":[{"title":"Smells Like Teen Spirit","length_ms":301000,"explicit":false,"album":"Nevermind","album_mbid":"$ALBUM"}],
    "popularity":{"monthly_listeners":12300000,"world_rank":42,"listeners_change_today":-12000},
    "similar":[],"related":[{"mbid":"r1","name":"Foo Fighters","image":"/x","note":"member of band"}],
    "kid":false,"missing":{"count":2,"size_bytes":943718400,"albums":["$ALBUM","22222222-2222-2222-2222-222222222222"]},
    "pictures":{"wide:$MBID":{"v":"1","w":1,"h":1,"color":"#d62839"}}}"""

private fun FakeServer.artistRoutes(
    titleId: Long = 0,
    ask: String = "",
) {
    on("GET", "/api/v1/music/artists/$MBID") { request ->
        when (request.url.parameters["part"]) {
            "similar" -> respondJson("""{"similar":[{"mbid":"s1","name":"Pearl Jam","image":"/s"}]}""")
            "more" ->
                respondJson(
                    """{"essential":["$ALBUM"],"appears_on":[{"mbid":"x","name":"Split","image":"/s","artist_mbid":"o","artist_name":"Other"}]}""",
                )
            "videos" -> respondJson("""{"videos":[{"id":"abc","title":"Nirvana - Come as You Are","duration":219}]}""")
            else ->
                respondJson(
                    if (titleId >
                        0
                    ) {
                        ARTIST_JSON.replaceFirst("\"have\":0", "\"title_id\":$titleId,\"have\":0")
                    } else {
                        ARTIST_JSON
                    },
                )
        }
    }
    json("GET", "/api/v1/artists/lookup", """{"artists":[{"mbid":"$MBID","name":"Nirvana","title_id":$titleId$ask}]}""")
    json("GET", "/api/v1/titles/7/follow", """{"title_id":7,"following":true}""")
}

class MusicPresenterTest {
    @Test
    fun artistPageBuildsShelvesAndTheMainAction() =
        runTest {
            val server = FakeServer()
            server.artistRoutes()
            val presenter = MusicArtistPresenter(server.api(), { testAccount(Role.MEMBER, listOf("ask")) }, MBID) { TODAY }
            presenter.load()
            val s = presenter.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertEquals("Nirvana", s.name)
            assertEquals(listOf(MusicShelfKind.ALBUMS, MusicShelfKind.LIVE), s.shelves.map { it.kind })
            assertEquals(
                listOf("Nevermind", "Bleach"),
                s.shelves
                    .first()
                    .albums
                    .map { it.name },
            )
            assertEquals(listOf("Next"), s.coming.map { it.name })
            assertEquals(listOf("Nevermind"), s.essential.map { it.name })
            assertEquals("Pearl Jam", s.similar.single().name)
            assertEquals("Split", s.appearsOn.single().name)
            assertEquals("Come as You Are", s.videos.single().title)
            assertEquals("3:39", s.videos.single().length)
            assertEquals(12_300_000, s.listeners)
            assertEquals(42, s.rank)
            assertEquals("CC BY-SA 4.0", s.bio?.license)
            assertEquals("1989", s.activeSince)
            assertEquals("1994", s.activeUntil)
            assertEquals(MusicMain.ASK, s.main)
            assertTrue(s.mainReady)
            assertEquals(2, s.askAll?.count)
            assertFalse(s.askAll!!.gb)
            assertEquals("900", s.askAll.size)
            assertTrue(s.tint != 0L)
        }

    @Test
    fun managersAddAndLibraryArtistsOpenTheLibrary() =
        runTest {
            val manager = FakeServer()
            manager.artistRoutes()
            val add = MusicArtistPresenter(manager.api(), { testAccount() }, MBID) { TODAY }
            add.load()
            assertEquals(MusicMain.ADD, add.state.value.main)
            assertNull(add.state.value.askAll)

            val library = FakeServer()
            library.artistRoutes(titleId = 7)
            val open = MusicArtistPresenter(library.api(), { testAccount() }, MBID) { TODAY }
            open.load()
            assertEquals(MusicMain.OPEN_LIBRARY, open.state.value.main)
            assertTrue(open.state.value.followKnown)
            assertTrue(open.state.value.following)
        }

    @Test
    fun askedArtistsShowAsAsked() =
        runTest {
            val server = FakeServer()
            server.artistRoutes(ask = ""","ask":{"id":3,"state":"asked","mine":true,"albums":[]}""")
            val presenter = MusicArtistPresenter(server.api(), { testAccount(Role.MEMBER, listOf("ask")) }, MBID) { TODAY }
            presenter.load()
            assertEquals(MusicMain.ASKED, presenter.state.value.main)
            assertNull(presenter.state.value.askAll)
        }

    @Test
    fun askAllPostsTheMissingAlbums() =
        runTest {
            val server = FakeServer()
            server.artistRoutes()
            server.json("POST", "/api/v1/asks", """{"ask":{"id":5,"kind":"artist","state":"asked"},"joined":false,"already":false}""", 201)
            server.json("GET", "/api/v1/asks/messages", """{"messages":[],"unread":0}""")
            val presenter = MusicArtistPresenter(server.api(), { testAccount(Role.MEMBER, listOf("ask")) }, MBID) { TODAY }
            presenter.load()
            presenter.askAll()
            val body = server.request("POST", "/api/v1/asks").body.orEmpty()
            assertTrue(body.contains("\"kind\":\"artist\""))
            assertTrue(body.contains("mbid:$MBID"))
            assertTrue(body.contains("22222222-2222-2222-2222-222222222222"))
            assertEquals(
                MusicNoteKind.ASKED_REQUESTED,
                presenter.state.value.note
                    ?.kind,
            )
        }

    @Test
    fun followingAnUpcomingAlbumRollsBackOnFailure() =
        runTest {
            val server = FakeServer()
            server.artistRoutes()
            server.json(
                "PUT",
                "/api/v1/metadata/album/44444444-4444-4444-4444-444444444444/follow",
                """{"error":"that album is out already"}""",
                409,
            )
            val presenter = MusicArtistPresenter(server.api(), { testAccount() }, MBID) { TODAY }
            presenter.load()
            presenter.toggleAlbumFollow("44444444-4444-4444-4444-444444444444")
            assertFalse(
                presenter.state.value.coming
                    .single()
                    .following,
            )
            assertEquals(
                MusicNoteKind.FOLLOW_FAILED,
                presenter.state.value.note
                    ?.kind,
            )
            val body = server.request("PUT", "/api/v1/metadata/album/44444444-4444-4444-4444-444444444444/follow").body.orEmpty()
            assertTrue(body.contains("\"artist\":\"Nirvana\""))
            assertTrue(body.contains("\"release_date\":\"2027-02-01\""))
        }

    @Test
    fun missingArtistsFailWithAProblem() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/music/artists/$MBID", """{"error":"no artist with that id"}""", 404)
            val presenter = MusicArtistPresenter(server.api(), { testAccount() }, MBID) { TODAY }
            presenter.load()
            assertEquals(Phase.FAILED, presenter.state.value.status.phase)
            assertEquals(ch.orellbuehler.acolyte.feature.music.MusicProblem.NOT_FOUND, presenter.state.value.problem)
        }

    @Test
    fun albumPageShowsTracksEditionsAndTheCta() =
        runTest {
            val server = FakeServer()
            val album =
                """{"mbid":"$ALBUM","name":"Nevermind","type":"album","release_date":"1991-09-24","image":"/i","artist":{"mbid":"$MBID","name":"Nirvana"},
                "title_id":0,"label":"DGC","length_ms":600000,"explicit":false,
                "tracks":[{"disc":1,"number":1,"name":"Come as You Are","length_ms":219000,"have":"in_library"},{"disc":1,"number":2,"name":"Lithium","length_ms":257000,"have":"missing"}],
                "editions":[{"mbid":"e1","label":"Deluxe","tracks":3,"list":[{"disc":1,"number":1,"name":"A"},{"disc":1,"number":2,"name":"B"},{"disc":2,"number":1,"name":"C"}]}],
                "more_by":[{"mbid":"m1","name":"Bleach","image":"/b"}],"part_id":0}"""
            server.on("GET", "/api/v1/music/albums/$ALBUM") { request ->
                if (request.url.parameters["part"] ==
                    "near"
                ) {
                    respondJson("""{"near":[{"mbid":"n1","name":"Ten","image":"/t","artist_name":"Pearl Jam"}]}""")
                } else {
                    respondJson(album)
                }
            }
            server.json("GET", "/api/v1/music/artists/$MBID", """{"similar":[{"mbid":"s1","name":"Pearl Jam"}]}""")
            server.json("GET", "/api/v1/artists/lookup", """{"artists":[{"mbid":"$MBID","name":"Nirvana"}]}""")
            val presenter = MusicAlbumPresenter(server.api(), { testAccount(Role.MEMBER, listOf("ask")) }, ALBUM, MBID)
            presenter.load()
            var s = presenter.state.value
            assertEquals("Nevermind", s.name)
            assertEquals("Nirvana", s.artistName)
            assertEquals(2, s.songs)
            assertEquals(listOf(MusicHave.IN_LIBRARY, MusicHave.MISSING), s.tracks.map { it.have })
            assertEquals(MusicAlbumCta.ASK, s.cta)
            assertEquals("Ten", s.near.single().name)
            assertEquals(1, s.moreBy.size)
            presenter.pickEdition("e1")
            s = presenter.state.value
            assertEquals(3, s.songs)
            assertEquals(2, s.discs)
            assertEquals("e1", s.edition)
        }

    @Test
    fun exploreRowsKeepRisingAndPageThroughCursors() =
        runTest {
            val server = FakeServer()
            val card =
                """{"key":"album:a","kind":"album","mbid":"$ALBUM","name":"Nevermind","artist":"Nirvana",""" +
                    """"artist_mbid":"$MBID","image":"/i","date":"2026-11-20"}"""
            server.on("GET", "/api/v1/explore/music") { request ->
                if (request.url.parameters["cursor"] == "c1") {
                    respondJson("""{"rows":[{"id":"r3","type":"mix","title":"Three","shape":"square","items":[$card]}],"next":""}""")
                } else {
                    respondJson(
                        """{"kid":false,"region":"US","regions":["US","global"],"hero":$card,"hero_alts":[],"next":"c1",
                        "rows":[{"id":"r1","type":"fresh","title":"One","reason":"New","shape":"square","items":[$card]},
                        {"id":"t","type":"genres","title":"Genres","shape":"tile","items":[{"key":"genre:pop","kind":"genre","mbid":"pop","name":"Pop","hue":3}]},
                        {"id":"r2","type":"trending_artists","title":"Two","shape":"round","items":[$card]}]}""",
                    )
                }
            }
            server.json(
                "GET",
                "/api/v1/explore/music/rising",
                """{"id":"rising_today","type":"rising_today","title":"Rising today","shape":"round","items":[$card]}""",
            )
            val presenter = MusicRowsPresenter(server.api(), { testAccount(Role.MEMBER, listOf("ask")) }, "", "", "US") { TODAY }
            presenter.load()
            var s = presenter.state.value
            assertEquals(listOf("r1", "r2", "rising_today"), s.rows.map { it.id })
            assertEquals(1, s.strip.size)
            assertEquals("pop", s.strip.single().mbid)
            assertEquals(
                MusicCardAction.ASK,
                s.rows
                    .first()
                    .items
                    .first()
                    .action,
            )
            assertEquals("Nevermind", s.hero?.name)
            assertTrue(s.more)
            presenter.loadMore()
            s = presenter.state.value
            assertEquals(listOf("r1", "r2", "rising_today", "r3"), s.rows.map { it.id })
            assertFalse(s.more)
        }

    @Test
    fun genrePagesUseTheTileBand() =
        runTest {
            val server = FakeServer()
            server.json(
                "GET",
                "/api/v1/music/genre/pop",
                """{"tile":{"key":"genre:pop","kind":"genre","name":"Pop","hue":2},"hero":null,"rows":[{"id":"g","type":"genre_top","title":"Top","shape":"square","items":[{"key":"k","kind":"album","mbid":"x","name":"X","artist":"Ann"}]}]}""",
            )
            val presenter = MusicRowsPresenter(server.api(), { testAccount() }, "genre", "pop", "global") { TODAY }
            presenter.load()
            val s = presenter.state.value
            assertEquals("Pop", s.title)
            assertEquals("genre", s.tileKind)
            assertEquals(30, s.bandHue)
            assertEquals(listOf("Ann"), s.leads)
            assertEquals("global", server.request("GET", "/api/v1/music/genre/pop").query.substringAfter("region="))
        }

    @Test
    fun kidsGetNoRows() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/explore/music", """{"kid":true,"rows":[]}""")
            val presenter = MusicRowsPresenter(server.api(), { testAccount(kid = true) }, "", "", "US") { TODAY }
            presenter.load()
            assertTrue(presenter.state.value.kid)
            assertFalse(presenter.state.value.isEmpty)
        }

    @Test
    fun newMusicInsertsRisingAndFollowsUpcomingAlbums() =
        runTest {
            val server = FakeServer()
            val card =
                """{"key":"a","kind":"album","mbid":"$ALBUM","name":"Soon","artist":"Ann","artist_mbid":"$MBID",""" +
                    """"image":"/i","date":"2027-01-01","upcoming":true}"""
            server.json(
                "GET",
                "/api/v1/explore/new/music",
                """{"kid":false,"building":false,"hero":[$card],"genres":["pop"],"rows":[{"id":"np_music_week","type":"x","title":"Out this week","shape":"square","items":[$card]},
                {"id":"np_music_coming","type":"x","title":"Coming","shape":"square","items":[$card]}]}""",
            )
            server.json(
                "GET",
                "/api/v1/explore/music/rising",
                """{"id":"rising_today","type":"rising_today","title":"Rising","shape":"round","items":[$card]}""",
            )
            server.json(
                "GET",
                "/api/v1/music/browse-tiles",
                """{"sections":[],"explore":[{"key":"genre:pop","name":"Pop","href":"/music/genre/pop","covers":["/c"]}]}""",
            )
            server.json("PUT", "/api/v1/metadata/album/$ALBUM/follow", """{"mbid":"$ALBUM","following":true}""")
            val presenter = MusicNewPresenter(server.api(), { testAccount() }, "US") { TODAY }
            presenter.load()
            var s = presenter.state.value
            assertEquals(listOf("np_music_week", "rising_today", "np_music_coming"), s.rows.map { it.id })
            assertEquals("Pop", s.tiles.single().name)
            assertEquals(MusicCardAction.NONE, s.hero.single().action)
            presenter.toggleFollow("a")
            s = presenter.state.value
            assertTrue(s.hero.single().following)
            assertTrue(
                server
                    .request("PUT", "/api/v1/metadata/album/$ALBUM/follow")
                    .body
                    .orEmpty()
                    .contains("2027-01-01"),
            )
        }

    @Test
    fun browseTilesFilterEmptySections() =
        runTest {
            val server = FakeServer()
            server.json(
                "GET",
                "/api/v1/music/browse-tiles",
                """{"sections":[{"id":"genres","title":"Genres","tiles":[{"key":"genre:pop","name":"Pop","href":"/music/genre/pop"}]},{"id":"moods","title":"Moods","tiles":[]}]}""",
            )
            val presenter = MusicBrowsePresenter(server.api(), "global")
            presenter.load()
            assertEquals(
                listOf("genres"),
                presenter.state.value.sections
                    .map { it.id },
            )
        }

    @Test
    fun artistSearchOffersAddOrAsk() =
        runTest {
            val server = FakeServer()
            server.json(
                "GET",
                "/api/v1/artists/lookup",
                """{"artists":[{"mbid":"a","name":"A"},{"mbid":"b","name":"B","title_id":3},{"mbid":"c","name":"C","ask":{"id":1,"mine":false}},{"mbid":"d","name":"D","never_add":true}]}""",
            )
            val member = MusicSearchPresenter(server.api()) { testAccount(Role.MEMBER, listOf("ask")) }
            member.search("band")
            assertEquals(
                listOf(MusicCardAction.ASK, MusicCardAction.NONE, MusicCardAction.ASK, MusicCardAction.NONE),
                member.state.value.hits
                    .map { it.action },
            )
            assertTrue(
                member.state.value.hits[2]
                    .tooAction,
            )
            val manager = MusicSearchPresenter(server.api()) { testAccount() }
            manager.search("band")
            assertEquals(
                MusicCardAction.ADD,
                manager.state.value.hits
                    .first()
                    .action,
            )
        }

    @Test
    fun addArtistSendsEveryChoice() =
        runTest {
            val server = FakeServer()
            server.json(
                "GET",
                "/api/v1/artists/lookup",
                """{"artists":[{"mbid":"$MBID","name":"Nirvana","disambiguation":"grunge band"}]}""",
            )
            server.json(
                "GET",
                "/api/v1/library-folders",
                """{"library_folders":[{"id":1,"kind":"movie","path":"/m"},{"id":4,"kind":"music","path":"/music","label":"Music","health":"ok","default_pick_rules_id":8}]}""",
            )
            server.json(
                "GET",
                "/api/v1/pick-rules",
                """{"pick_rules":[{"id":2,"name":"4K","kinds":["movie"]},{"id":8,"name":"Lossless first","kinds":["music"]}]}""",
            )
            server.json(
                "GET",
                "/api/v1/settings/music",
                """{"album_choice":"latest","new_albums":"new","album_types":["album"],"secondary_types":["studio","live"],"release_statuses":["official"]}""",
            )
            server.json("POST", "/api/v1/artists", """{"title":{"id":77,"kind":"artist","name":"Nirvana"},"job_id":3}""", 201)
            val presenter = MusicAddPresenter(server.api(), { testAccount() }, MBID, "Nirvana")
            presenter.load()
            var s = presenter.state.value
            assertEquals(4, s.folderId)
            assertEquals(8, s.rulesId)
            assertEquals("grunge band", s.disambiguation)
            assertEquals("latest", s.albumChoice)
            assertTrue(s.canSubmit)
            presenter.toggleAlbumType("ep", true)
            presenter.setFollow(false)
            presenter.submit()
            s = presenter.state.value
            assertEquals(MusicAddOutcome.ADDED, s.outcome)
            assertEquals(77, s.addedTitleId)
            val body = server.request("POST", "/api/v1/artists").body.orEmpty()
            assertTrue(body.contains("\"id\":\"mbid:$MBID\""))
            assertTrue(body.contains("\"follow\":false"))
            assertTrue(body.contains("\"library_folder_id\":4"))
            assertTrue(body.contains("\"album_types\":[\"album\",\"ep\"]"))
            assertTrue(body.contains("\"albums\":\"latest\""))
        }

    @Test
    fun addArtistBlocksWithoutAFolder() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/artists/lookup", """{"artists":[]}""")
            server.json("GET", "/api/v1/library-folders", """{"library_folders":[]}""")
            server.json("GET", "/api/v1/pick-rules", """{"pick_rules":[]}""")
            server.json("GET", "/api/v1/settings/music", """{}""")
            val presenter = MusicAddPresenter(server.api(), { testAccount() }, MBID, "Nirvana")
            presenter.load()
            assertTrue(presenter.state.value.noFolder)
            assertFalse(presenter.state.value.canSubmit)
            presenter.toggleAlbumType("album", false)
            presenter.toggleAlbumType("ep", false)
            assertFalse(presenter.state.value.canSubmit)
        }

    @Test
    fun musicSettingsEditAndSave() =
        runTest {
            val server = FakeServer()
            server.json(
                "GET",
                "/api/v1/settings/music",
                """{"album_choice":"all","new_albums":"all","album_types":["album","ep"],"secondary_types":["studio"],"release_statuses":["official"],"pressing_rule":"common","musicbrainz_url":"","musicbrainz_allow_local_network":true,"cache_hours":24}""",
            )
            server.on("PUT", "/api/v1/settings/music") { respondJson((it.body as TextContent).text) }
            val presenter = MusicSettingsPresenter(server.api()) { testAccount(Role.OWNER, emptyList()) }
            presenter.load()
            assertFalse(presenter.state.value.dirty)
            presenter.setAlbumChoice("none")
            presenter.setHours("48")
            presenter.toggleAlbumType("single", true)
            assertTrue(presenter.state.value.canSave)
            presenter.toggleAlbumType("album", false)
            presenter.toggleAlbumType("ep", false)
            presenter.toggleAlbumType("single", false)
            assertTrue(presenter.state.value.emptyKinds)
            assertFalse(presenter.state.value.canSave)
            presenter.discard()
            presenter.setAlbumChoice("none")
            presenter.setHours("720")
            presenter.save()
            val body = server.request("PUT", "/api/v1/settings/music").body.orEmpty()
            assertTrue(body.contains("\"album_choice\":\"none\""))
            assertTrue(body.contains("\"cache_hours\":720"))
            assertTrue(body.contains("\"musicbrainz_allow_local_network\":true"))
            assertFalse(presenter.state.value.dirty)
            assertTrue(presenter.state.value.justSaved)
            presenter.setHours("900")
            assertFalse(presenter.state.value.hoursValid)
        }

    @Test
    fun unmatchedNoteNeedsAManager() =
        runTest {
            val server = FakeServer()
            server.json(
                "GET",
                "/api/v1/library/unmatched-music",
                """{"total":7,"artists":[{"title_id":1,"name":"A","files":4},{"title_id":2,"name":"B","files":3}]}""",
            )
            val member = MusicUnmatchedPresenter(server.api()) { testAccount(Role.MEMBER, listOf("ask")) }
            member.load()
            assertEquals(0, member.state.value.total)
            val manager = MusicUnmatchedPresenter(server.api()) { testAccount() }
            manager.load()
            assertEquals(7, manager.state.value.total)
            assertEquals(2, manager.state.value.shown.size)
        }
}

private const val TITLE = 7L

private fun FakeServer.libraryRoutes() {
    json(
        "GET",
        "/api/v1/titles/$TITLE",
        """{"id":$TITLE,"kind":"artist","name":"Nirvana","keep_up_to_date":true,"folder_path":"/music/Nirvana","library_folder_id":4,"pick_rules_id":8,"pick_rules_name":"Lossless first",
        "artwork":["/a"],"asks":[{"id":1,"state":"asked","state_label":"Asked","asked_by":{"id":2,"name":"bo"},"asked_at":"2026-11-20T10:00:00Z","album_words":"Nevermind"}],
        "artist":{"mbid":"$MBID","status":"ended","albums":2,"included":2,"tracks":12,"in_library":9,"missing":2,"not_out_yet":1,"new_albums":"all","album_types":["album","ep"],"secondary_types":["studio"],"release_statuses":["official"]}}""",
    )
    json(
        "GET",
        "/api/v1/titles/$TITLE/albums",
        """{"albums":[
        {"id":11,"mbid":"$ALBUM","name":"Nevermind","type":"album","release_date":"1991-09-24T00:00:00Z","included":true,"keep_up_to_date":true,"tracks":12,"in_library":9,"missing":2,"state":"partial"},
        {"id":12,"mbid":"x","name":"Live","type":"album","secondary_types":["live"],"release_date":"1994-11-01T00:00:00Z","included":false,"state":"not_included"}]}""",
    )
    json(
        "GET",
        "/api/v1/pick-rules",
        """{"pick_rules":[{"id":7,"name":"Any music","kinds":["music"]},{"id":8,"name":"Lossless first","kinds":["music"]},{"id":9,"name":"Mine","kinds":["music"]}]}""",
    )
    json(
        "GET",
        "/api/v1/library-folders",
        """{"library_folders":[{"id":4,"kind":"music","path":"/music"},{"id":5,"kind":"music","path":"/music2","label":"Two"}]}""",
    )
    json("GET", "/api/v1/library/unmatched-music", """{"total":3,"artists":[{"title_id":$TITLE,"name":"Nirvana","files":3}]}""")
}

class MusicLibraryPresenterTest {
    @Test
    fun libraryArtistGroupsAlbumsAndReadsChoices() =
        runTest {
            val server = FakeServer()
            server.libraryRoutes()
            val presenter = MusicLibraryArtistPresenter(server.api(), { testAccount() }, TITLE) { TODAY }
            presenter.load()
            var s = presenter.state.value
            assertEquals("Nirvana", s.name)
            assertEquals(9, s.haveTracks)
            assertEquals(12, s.totalTracks)
            assertEquals(MusicKindsPresetCheck.ALBUMS_EPS, MusicKindsPresetCheck.of(s.preset.name))
            assertEquals(listOf("Nevermind"), s.shown.map { it.name })
            assertEquals(1, s.leftOut)
            assertEquals(3, s.unmatched)
            assertEquals(listOf(7L, 8L), s.quick.map { it.id })
            assertFalse(s.customQuality)
            assertEquals("Nevermind", s.asks.single().albums)
            presenter.setFilter(ch.orellbuehler.acolyte.feature.music.MusicLibFilter.ALL)
            assertEquals(2, presenter.state.value.shown.size)
            s = presenter.state.value
            assertEquals(2, s.folders.size)
        }

    @Test
    fun togglingAnAlbumSendsKeepUpToDate() =
        runTest {
            val server = FakeServer()
            server.libraryRoutes()
            server.on("PUT", "/api/v1/albums/11") { respondJson("""{"id":11,"keep_up_to_date":false}""") }
            val presenter = MusicLibraryArtistPresenter(server.api(), { testAccount() }, TITLE) { TODAY }
            presenter.load()
            presenter.toggleKeepAlbum(11)
            assertTrue(
                server
                    .request("PUT", "/api/v1/albums/11")
                    .body
                    .orEmpty()
                    .contains("\"keep_up_to_date\":false"),
            )
            assertEquals(
                MusicNoteKind.KEEP_ALBUM_OFF,
                presenter.state.value.note
                    ?.kind,
            )
            presenter.toggleKeepAlbum(12)
            assertEquals(1, server.requests.count { it.method == "PUT" && it.path.startsWith("/api/v1/albums") })
        }

    @Test
    fun savingKindsSendsAllFourGroups() =
        runTest {
            val server = FakeServer()
            server.libraryRoutes()
            server.json("PUT", "/api/v1/titles/$TITLE/artist", """{"title":{"id":$TITLE},"job_id":2}""")
            val presenter = MusicLibraryArtistPresenter(server.api(), { testAccount() }, TITLE) { TODAY }
            presenter.load()
            assertFalse(presenter.saveKinds("all", emptyList(), listOf("studio"), listOf("official")))
            assertTrue(presenter.saveKinds("new", listOf("album"), listOf("studio", "live"), listOf("official")))
            val body = server.request("PUT", "/api/v1/titles/$TITLE/artist").body.orEmpty()
            assertTrue(body.contains("\"new_albums\":\"new\""))
            assertTrue(body.contains("\"secondary_types\":[\"studio\",\"live\"]"))
            presenter.applyPreset(ch.orellbuehler.acolyte.feature.music.MusicKindsPreset.ALBUMS_EPS_SINGLES)
            assertTrue(
                server
                    .request(
                        "PUT",
                        "/api/v1/titles/$TITLE/artist",
                    ).body
                    .orEmpty()
                    .contains("\"album_types\":[\"album\",\"ep\",\"single\"]"),
            )
        }

    @Test
    fun searchBodiesScopeToAlbumsAndTracks() =
        runTest {
            val server = FakeServer()
            server.libraryRoutes()
            server.json("POST", "/api/v1/titles/$TITLE/search", """{"job_ids":[1]}""")
            val presenter = MusicLibraryArtistPresenter(server.api(), { testAccount() }, TITLE) { TODAY }
            presenter.load()
            presenter.searchAlbum(11)
            assertTrue(
                server
                    .request("POST", "/api/v1/titles/$TITLE/search")
                    .body
                    .orEmpty()
                    .contains("\"album_id\":11"),
            )
            presenter.searchTrack(11, 5, "Lithium")
            assertTrue(
                server
                    .request("POST", "/api/v1/titles/$TITLE/search")
                    .body
                    .orEmpty()
                    .contains("\"slot_id\":5"),
            )
            assertEquals(
                MusicNoteKind.SEARCHING,
                presenter.state.value.note
                    ?.kind,
            )
        }

    @Test
    fun expandingAnAlbumLoadsItsTracksOnce() =
        runTest {
            val server = FakeServer()
            server.libraryRoutes()
            server.json(
                "GET",
                "/api/v1/albums/11/tracks",
                """{"album_id":11,"tracks":[{"id":1,"disc":1,"number":1,"name":"Lithium","length_ms":257000,"slots":[{"id":5,"version":"Main","state":"in_library","state_label":"In library","file_id":3}]},
                {"id":2,"disc":1,"number":2,"name":"","dropped":true}]}""",
            )
            val presenter = MusicLibraryArtistPresenter(server.api(), { testAccount() }, TITLE) { TODAY }
            presenter.load()
            presenter.expand(11)
            val tracks = presenter.state.value.tracksOf(11L)
            assertEquals(1, tracks.size)
            assertEquals("In library", tracks.single().stateLabel)
            presenter.expand(11)
            presenter.expand(11)
            assertEquals(1, server.requests.count { it.path == "/api/v1/albums/11/tracks" })
        }

    @Test
    fun libraryAlbumReadsPressingsAndTracks() =
        runTest {
            val server = FakeServer()
            server.libraryRoutes()
            server.json(
                "GET",
                "/api/v1/albums/11",
                """{"album":{"id":11,"name":"Nevermind","type":"album","release_date":"1991-09-24T00:00:00Z","included":true,"keep_up_to_date":true,"tracks":12,"in_library":12,"state":"complete"},
                "title_id":$TITLE,"pressings":[{"id":1,"name":"Nevermind","country":"US","date":"1991-09-24","formats":["CD","Digital Media"],"track_count":12,"chosen":true,"chosen_by":"auto"},
                {"id":2,"name":"Nevermind","track_count":13}]}""",
            )
            server.json(
                "GET",
                "/api/v1/albums/11/tracks",
                """{"album_id":11,"tracks":[{"id":1,"disc":1,"number":1,"name":"Lithium","length_ms":257000,"slots":[{"id":5,"state":"in_library","state_label":"In library","file_id":3}]}]}""",
            )
            server.on("PUT", "/api/v1/albums/11") { respondJson("""{"id":11}""") }
            val presenter = MusicLibraryAlbumPresenter(server.api(), { testAccount() }, TITLE, 11) { TODAY }
            presenter.load()
            var s = presenter.state.value
            assertEquals("Nirvana", s.artistName)
            assertEquals(1991.toString(), s.year)
            assertTrue(s.byRule)
            assertEquals("CD + Digital Media", s.pressings.first().formats)
            assertTrue(s.hasFile)
            assertTrue(s.complete)
            presenter.changePressing(2)
            assertTrue(
                server
                    .request("PUT", "/api/v1/albums/11")
                    .body
                    .orEmpty()
                    .contains("\"pressing_id\":2"),
            )
            s = presenter.state.value
            assertEquals(MusicNoteKind.PRESSING_CHANGED, s.note?.kind)
        }

    @Test
    fun missingLibraryAlbumsAreNotFound() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/albums/11", """{"error":"no such album"}""", 404)
            val presenter = MusicLibraryAlbumPresenter(server.api(), { testAccount() }, TITLE, 11) { TODAY }
            presenter.load()
            assertTrue(presenter.state.value.notFound)
        }

    @Test
    fun unmatchedFilesMatchToATrackAndReplace() =
        runTest {
            val server = FakeServer()
            server.json(
                "GET",
                "/api/v1/titles/$TITLE/leftovers",
                """{"leftovers":[{"file_id":31,"relative_path":"Nirvana/x/01 song.flac","size":1234,"reason":"music_no_track","music":{"album_id":11,"album_name":"Nevermind"},"dismissed":false},
                {"file_id":32,"relative_path":"b.flac","reason":"mystery","dismissed":true},
                {"file_id":33,"relative_path":"c.flac","reason":"music_two_album","dismissed":false}],
                "pressings":[{"album_id":11,"album_name":"Nevermind","pressing_id":2,"pressing_name":"Deluxe","track_count":13,"fits":9,"chosen":3,"files":9}]}""",
            )
            server.json(
                "GET",
                "/api/v1/titles/$TITLE/albums",
                """{"albums":[{"id":11,"name":"Nevermind","included":true},{"id":12,"name":"Live","included":false}]}""",
            )
            server.json(
                "GET",
                "/api/v1/albums/11/tracks",
                """{"tracks":[{"id":100,"disc":1,"number":1,"name":"A","slots":[{"id":1,"file_id":9}]},{"id":101,"disc":1,"number":2,"name":"B","slots":[{"id":2}]}]}""",
            )
            server.json("POST", "/api/v1/titles/$TITLE/leftovers/match", """{"results":[{"file_id":31,"linked":true}]}""")
            server.json("POST", "/api/v1/titles/$TITLE/leftovers/unmatch", """{"unmatched":1}""")
            val presenter = MusicLeftoversPresenter(server.api(), TITLE) {}
            presenter.load()
            var s = presenter.state.value
            assertEquals(2, s.open.size)
            assertEquals("music_no_album", s.alone.single().reason)
            assertEquals(9, s.pressings.single().fits)
            presenter.openMatch(31)
            s = presenter.state.value
            assertEquals(listOf(11L), s.match?.albums?.map { it.id })
            assertEquals(2, s.match?.tracks?.size)
            presenter.pickTrack(100)
            assertTrue(
                presenter.state.value.match
                    ?.replaces == true,
            )
            presenter.confirmMatch()
            val body = server.request("POST", "/api/v1/titles/$TITLE/leftovers/match").body.orEmpty()
            assertTrue(body.contains("\"episode_ids\":[100]"))
            assertTrue(body.contains("\"replace\":true"))
            assertEquals(
                MusicNoteKind.MATCHED,
                presenter.state.value.note
                    ?.kind,
            )
            presenter.undoMatch()
            assertEquals(
                MusicNoteKind.UNMATCHED,
                presenter.state.value.note
                    ?.kind,
            )
        }
}

private object MusicKindsPresetCheck {
    const val ALBUMS_EPS = "ALBUMS_EPS"

    fun of(name: String): String = name
}
