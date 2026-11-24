package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ApiJson
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.quality.SettingsPickRuleEditPresenter
import ch.orellbuehler.acolyte.feature.quality.SettingsPickRulesPresenter
import ch.orellbuehler.acolyte.session.Role
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QualitySetsPresenterTest {
    private val rules =
        """{"pick_rules":[{"id":1,"name":"Standard","kinds":["movie","show"],"preset":true,"in_use":3},
            {"id":2,"name":"Mine","kinds":[],"preset":false,"in_use":4}]}"""

    private val traits =
        """{"own_traits":[{"id":8,"key":"own:8","name":"Big","match":"size","terms":["60-"]},
            {"id":7,"key":"own:7","name":"Subs","match":"name","terms":["NLSubs","/nl.?subs/"]},
            {"id":9,"key":"own:9","name":"T1","match":"name","terms":[],"source":"trash","specs":[{"name":"x","kind":"name","value":"v","negate":true}]},
            {"id":10,"key":"own:10","name":"Old","match":"name","terms":[],"source":"import","app":"sonarr"}]}"""

    private fun listServer(): FakeServer {
        val server = FakeServer()
        server.json("GET", "/api/v1/pick-rules", rules)
        server.json("GET", "/api/v1/own-traits", traits)
        return server
    }

    private val owner = { testAccount(Role.OWNER, emptyList()) }

    @Test
    fun listLoadsRulesAndGroupsTraits() =
        runTest {
            val presenter = SettingsPickRulesPresenter(listServer().api(), owner)
            presenter.load()
            val s = presenter.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertTrue(s.mayEdit)
            assertEquals(listOf("Standard", "Mine"), s.rules.map { it.name })
            assertEquals(listOf(true, false), s.rules.map { it.readyChip })
            assertEquals(4, s.rules[1].inUse)
            assertEquals(listOf("Big", "Subs"), s.handMade.map { it.name })
            assertEquals("60", s.handMade[0].over)
            assertEquals("", s.handMade[0].upTo)
            assertEquals(listOf("T1"), s.trashTraits.map { it.name })
            assertTrue(s.trashTraits.single().specs.single().negate)
            assertEquals(listOf("Old"), s.importedTraits.map { it.name })
            assertEquals("sonarr", s.importedTraits.single().app)
        }

    @Test
    fun presetsOnlyHaveNoChipAndViewersCannotEdit() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/pick-rules", """{"pick_rules":[{"id":1,"name":"Standard","kinds":["movie"],"preset":true,"in_use":0}]}""")
            server.json("GET", "/api/v1/own-traits", """{"own_traits":[]}""")
            val viewer = SettingsPickRulesPresenter(server.api()) { testAccount(Role.ADMIN, listOf("manage_library")) }
            viewer.load()
            val s = viewer.state.value
            assertFalse(s.mayEdit)
            assertFalse(s.rules.single().readyChip)
            assertTrue(s.traitsEmpty)
            viewer.askRemove(1)
            viewer.openTrait(0)
            assertNull(viewer.state.value.remove)
            assertNull(viewer.state.value.traitSheet)
            val none = SettingsPickRulesPresenter(server.api()) { testAccount(Role.ADMIN, listOf("ask")) }
            none.load()
            assertFalse(none.state.value.allowed)
        }

    @Test
    fun removeShowsTheInUseRefusalThenSucceeds() =
        runTest {
            val server = listServer()
            var calls = 0
            server.on("DELETE", "/api/v1/pick-rules/2") {
                calls++
                if (calls == 1) {
                    respondJson(
                        """{"error":"These Pick rules are used by 2 Versions and 1 Library folder","in_use":{"versions":2,"library_folders":1}}""",
                        409,
                    )
                } else {
                    respondJson("", 204)
                }
            }
            val presenter = SettingsPickRulesPresenter(server.api(), owner)
            presenter.load()
            presenter.askRemove(1)
            assertNull(presenter.state.value.remove)
            presenter.askRemove(2)
            assertEquals(4, presenter.state.value.remove!!.inUse)
            presenter.confirmRemove()
            val refused = presenter.state.value.remove!!
            assertEquals(listOf("versions" to 2, "library_folders" to 1), refused.use!!.parts.map { it.key to it.count })
            assertNotNull(refused.error)
            presenter.confirmRemove()
            val s = presenter.state.value
            assertNull(s.remove)
            assertEquals(listOf("Standard"), s.rules.map { it.name })
            assertEquals("removed", s.notice!!.kind)
            assertEquals("Mine", s.notice!!.name)
        }

    @Test
    fun ownTraitsMakeChangeAndRemove() =
        runTest {
            val server = listServer()
            server.on("POST", "/api/v1/own-traits") { respondJson("""{"id":11,"key":"own:11","name":"Anime","match":"group","terms":["A","B"]}""", 201) }
            server.on("PUT", "/api/v1/own-traits/8") { respondJson("""{"id":8,"key":"own:8","name":"Big","match":"size","terms":["2-8"]}""") }
            server.on("DELETE", "/api/v1/own-traits/7") { respondJson("""{"pick_rules_changed":2}""") }
            val presenter = SettingsPickRulesPresenter(server.api(), owner)
            presenter.load()
            presenter.openTrait(0)
            presenter.setTraitName("Anime")
            presenter.setTraitMatch("group")
            presenter.saveTrait()
            var sheet = presenter.state.value.traitSheet!!
            assertEquals("terms_min", sheet.problem("terms")!!.code)
            presenter.setTraitTerms("A\n\n  B \n")
            assertNull(presenter.state.value.traitSheet!!.problem("terms"))
            presenter.saveTrait()
            val s = presenter.state.value
            assertNull(s.traitSheet)
            assertEquals("trait_made", s.notice!!.kind)
            assertEquals(listOf("Anime", "Big", "Subs"), s.handMade.map { it.name })
            val body = server.request("POST", "/api/v1/own-traits").body.orEmpty()
            assertTrue(body.contains(""""terms":["A","B"]"""))
            assertTrue(body.contains(""""match":"group""""))
            presenter.openTrait(8)
            sheet = presenter.state.value.traitSheet!!
            assertEquals("size", sheet.match)
            assertEquals("60", sheet.over)
            presenter.setTraitOver("2")
            presenter.setTraitUpTo("8")
            presenter.saveTrait()
            assertTrue(server.request("PUT", "/api/v1/own-traits/8").body.orEmpty().contains(""""terms":["2-8"]"""))
            presenter.setTraitMatch("size")
            presenter.askRemoveTrait(7)
            assertEquals("Subs", presenter.state.value.traitRemove!!.name)
            presenter.confirmRemoveTrait()
            val after = presenter.state.value
            assertNull(after.traitRemove)
            assertEquals(2, after.notice!!.count)
            assertEquals("trait_removed", after.notice!!.kind)
            assertFalse(after.handMade.any { it.id == 7L })
        }

    @Test
    fun ownTraitFieldErrorsMapBack() =
        runTest {
            val server = listServer()
            server.on("POST", "/api/v1/own-traits") {
                respondJson("""{"error":"Another of your traits already has this name.","field":"name"}""", 409)
            }
            val presenter = SettingsPickRulesPresenter(server.api(), owner)
            presenter.load()
            presenter.openTrait(0)
            presenter.setTraitName("Big")
            presenter.setTraitTerms("x")
            presenter.saveTrait()
            val sheet = presenter.state.value.traitSheet!!
            assertEquals("server", sheet.problem("name")!!.code)
            assertEquals("Another of your traits already has this name.", sheet.problem("name")!!.arg)
            assertFalse(sheet.busy)
            presenter.setTraitMatch("size")
            presenter.setTraitOver("5")
            presenter.setTraitUpTo("2")
            presenter.saveTrait()
            assertEquals("size_range", presenter.state.value.traitSheet!!.problem("terms")!!.code)
        }

    @Test
    fun nameCheckSendsBytesAndMapsTheAnswer() =
        runTest {
            val server = listServer()
            server.on("POST", "/api/v1/releases/name-check") {
                respondJson(
                    """{"name":"Movie.2024.1080p.WEB-DL-GRP","parsed":{"title":{"value":"Movie","conf":"sure"},"year":{"value":2024,"conf":"sure"},
                    "group":{"value":"GRP","conf":"sure"},"flags":["proper","hardcoded"],"show":{"seasons":[1],"episodes":[2,4]}},
                    "read":{"resolution":1080,"source":"web_dl","codec":"x265"},
                    "title":{"id":5,"name":"Movie","kind":"movie","year":2024,"sure":true,"detail":"matched by name"},
                    "copies":[{"version":"Main","pick_rules":"Standard","outcome":"chosen","passed":true,"points":12,"good_enough":true,"has_file":true,
                    "reasons":[{"code":"points.total","effect":"add","text":"12 points"}]},
                    {"pick_rules":"Lite","outcome":"refused","passed":false,"points":0,"good_enough":false,"has_file":false,"reasons":[]}]}""",
                )
            }
            val presenter = SettingsPickRulesPresenter(server.api(), owner)
            presenter.load()
            presenter.runCheck()
            assertNull(presenter.state.value.check.result)
            presenter.setCheckName(" Movie.2024.1080p.WEB-DL-GRP ")
            presenter.setCheckRules(2)
            presenter.setCheckSize("1,5")
            presenter.runCheck()
            val request = server.request("POST", "/api/v1/releases/name-check").body.orEmpty()
            val sent = ApiJson.lenient.parseToJsonElement(request).jsonObject
            assertEquals("Movie.2024.1080p.WEB-DL-GRP", sent["name"]!!.jsonPrimitive.content)
            assertEquals("1610612736", sent["size"]!!.jsonPrimitive.content)
            assertEquals("2", sent["pick_rules_id"]!!.jsonPrimitive.content)
            val r = presenter.state.value.check.result!!
            assertEquals("Movie", r.title)
            assertEquals(2024, r.year)
            assertTrue("1080p" in r.tags)
            assertTrue("S01E02-E04" in r.tags)
            assertTrue("hardcoded" in r.tags)
            assertTrue("-GRP" in r.tags)
            assertFalse("proper" in r.tags)
            assertEquals("Movie (2024)", r.matchName)
            assertEquals("", r.hint)
            assertEquals(listOf("would_choose", "refused"), r.copies.map { it.verdict })
            assertEquals(listOf(Tone.OK, Tone.FAIL), r.copies.map { it.tone })
            assertEquals("Main", r.copies[0].header)
            assertTrue(r.copies[0].showRules)
            assertEquals("Lite", r.copies[1].header)
            assertEquals(12, r.copies[0].points)
        }

    @Test
    fun nameCheckFailureAndHints() =
        runTest {
            val server = listServer()
            server.on("POST", "/api/v1/releases/name-check") { respondJson("""{"error":"no such Pick rules"}""", 404) }
            val presenter = SettingsPickRulesPresenter(server.api(), owner)
            presenter.load()
            presenter.setCheckName("x")
            presenter.runCheck()
            val c = presenter.state.value.check
            assertNotNull(c.error)
            assertNull(c.result)
            assertFalse(c.busy)
            server.on("POST", "/api/v1/releases/name-check") { respondJson("""{"name":"x","parsed":{},"title":null,"copies":[],"note":"no_title"}""") }
            presenter.runCheck()
            val r = presenter.state.value.check.result!!
            assertEquals("no_title", r.hint)
            assertEquals("", r.title)
        }

    private val detail =
        """{"id":4,"name":"Mine","kinds":["movie"],"preset":false,"revision":3,"body":$SERVER_BODY,
            "in_use":{"versions":1,"library_folders":0,"outside_lists":0,"wishlist":0,"title_defaults":0}}"""

    private fun editServer(): FakeServer {
        val server = FakeServer()
        server.json("GET", "/api/v1/own-traits", """{"own_traits":[{"id":3,"key":"own:3","name":"Subs","match":"name","terms":["x"]}]}""")
        server.json("GET", "/api/v1/pick-rules/4", detail)
        return server
    }

    @Test
    fun editorLoadsASavedSetAndSavesTheWholeBodyBack() =
        runTest {
            val server = editServer()
            server.on("PUT", "/api/v1/pick-rules/4") { respondJson(detail.replace("\"revision\":3", "\"revision\":4")) }
            val presenter = SettingsPickRuleEditPresenter(server.api(), owner, 4, 0)
            presenter.load()
            var s = presenter.state.value
            assertTrue(s.ready)
            assertFalse(s.dirty)
            assertEquals("Mine", s.savedName)
            assertEquals(1, s.use.versions)
            assertTrue(s.showTrash)
            assertEquals("HD", s.form.synced!!.name)
            assertEquals(listOf("1080p_web", "720p_web"), s.form.allowedBestFirst.map { it.quality })
            presenter.setName("Renamed")
            presenter.moveStep("720p_web", up = true)
            presenter.setStepAllowed("1080p_bluray", true)
            presenter.setGoodEnoughPoints("5x")
            assertTrue(presenter.state.value.dirty)
            presenter.save()
            s = presenter.state.value
            assertEquals(1, s.notice!!.seq)
            assertEquals("saved", s.notice!!.kind)
            assertFalse(s.dirty)
            val sent = ApiJson.lenient.parseToJsonElement(server.request("PUT", "/api/v1/pick-rules/4").body.orEmpty()).jsonObject
            assertEquals("Renamed", sent["name"]!!.jsonPrimitive.content)
            val body = sent["body"]!!.jsonObject
            assertEquals(listOf("1080p_web", "720p_web", "1080p_bluray"), body["steps"]!!.jsonArray.map { it.jsonObject["quality"]!!.jsonPrimitive.content })
            assertEquals("5", body["good_enough_points"]!!.jsonPrimitive.content)
            assertEquals(ApiJson.lenient.parseToJsonElement(SERVER_BODY).jsonObject["equal_steps"], body["equal_steps"])
            assertEquals(ApiJson.lenient.parseToJsonElement(SERVER_BODY).jsonObject["synced"], body["synced"])
            assertEquals("1", body["protocol"]!!.jsonObject["later"]!!.jsonPrimitive.content)
        }

    @Test
    fun editorSwitchesBetweenVideoAndMusicAndGuardsChanges() =
        runTest {
            val server = editServer()
            val presenter = SettingsPickRuleEditPresenter(server.api(), owner, 4, 0)
            presenter.load()
            presenter.setKind("show", true)
            assertEquals(listOf("movie", "show"), presenter.state.value.form.kinds)
            assertEquals(3, presenter.state.value.form.steps.size)
            presenter.setKind("music", true)
            var form = presenter.state.value.form
            assertEquals(listOf("music"), form.kinds)
            assertEquals(5, form.steps.size)
            assertEquals("lossless", form.goodEnoughAt)
            assertEquals("Mine", form.name)
            assertNull(form.synced)
            assertFalse(presenter.state.value.showTrash)
            presenter.discard()
            form = presenter.state.value.form
            assertEquals(listOf("movie"), form.kinds)
            assertFalse(presenter.state.value.dirty)
            presenter.addTrait()
            assertEquals(3, presenter.state.value.form.traits.size)
            assertEquals("ddp_audio", presenter.state.value.form.traits.last().trait)
            presenter.setTraitKey(2, "hevc")
            assertEquals("ddp_audio", presenter.state.value.form.traits.last().trait)
            presenter.removeTrait(2)
            presenter.setStepAllowed("1080p_web", false)
            assertEquals("720p_web", presenter.state.value.form.goodEnoughAt)
        }

    @Test
    fun editorShowsFieldErrorsAndUsageFromTheServer() =
        runTest {
            val server = editServer()
            server.on("PUT", "/api/v1/pick-rules/4") {
                respondJson("""{"error":"The preferred size for \"1080p_web\" must sit between its smallest and largest.","field":"body.size_limits"}""", 400)
            }
            val presenter = SettingsPickRuleEditPresenter(server.api(), owner, 4, 0)
            presenter.load()
            presenter.setName("")
            presenter.save()
            var s = presenter.state.value
            assertEquals("rules_name", s.problem("name")!!.code)
            assertEquals("name", s.firstProblem)
            assertFalse(s.saving)
            presenter.setName("Ok")
            presenter.save()
            s = presenter.state.value
            assertEquals("server", s.problem("size_limits")!!.code)
            assertTrue(s.problem("size_limits")!!.arg.startsWith("The preferred size"))
            server.on("PUT", "/api/v1/pick-rules/4") {
                respondJson("""{"error":"These Pick rules are used by 1 Version","in_use":{"versions":1}}""", 409)
            }
            presenter.save()
            s = presenter.state.value
            assertNull(s.problem("size_limits"))
            assertNotNull(s.formError)
            assertEquals(1, s.formUse!!.versions)
            server.on("PUT", "/api/v1/pick-rules/4") { respondJson("""{"error":"Other Pick rules already have this name.","field":"name"}""", 409) }
            presenter.save()
            assertEquals("Other Pick rules already have this name.", presenter.state.value.problem("name")!!.arg)
        }

    @Test
    fun editorCreatesNewSetsAndCopies() =
        runTest {
            val server = editServer()
            server.on("POST", "/api/v1/pick-rules") { respondJson(detail.replace("\"id\":4", "\"id\":9").replace("Mine", "Fresh"), 201) }
            val fresh = SettingsPickRuleEditPresenter(server.api(), owner, 0, 0)
            fresh.load()
            var s = fresh.state.value
            assertTrue(s.isNew)
            assertTrue(s.showSaveBar)
            assertFalse(s.dirty)
            assertEquals(listOf("movie", "show"), s.form.kinds)
            fresh.setName("Fresh")
            assertTrue(fresh.state.value.dirty)
            fresh.save()
            s = fresh.state.value
            assertEquals(9L, s.id)
            assertEquals("made", s.notice!!.kind)
            assertEquals("Fresh", s.savedName)
            val sent = ApiJson.lenient.parseToJsonElement(server.request("POST", "/api/v1/pick-rules").body.orEmpty()).jsonObject
            assertEquals(14, sent["body"]!!.jsonObject["steps"]!!.jsonArray.size)
            val copy = SettingsPickRuleEditPresenter(server.api(), owner, 0, 4)
            copy.load()
            s = copy.state.value
            assertTrue(s.isNew)
            assertEquals("Mine copy", s.form.name)
            assertEquals(0, s.use.total)
            assertEquals("HD", s.form.synced!!.name)
        }

    @Test
    fun editorStatesForMissingSetsAndViewers() =
        runTest {
            val server = editServer()
            val gone = SettingsPickRuleEditPresenter(server.api(), owner, 77, 0)
            gone.load()
            assertTrue(gone.state.value.gone)
            assertFalse(gone.state.value.ready)
            val viewer = SettingsPickRuleEditPresenter(server.api(), { testAccount(Role.ADMIN, listOf("manage_library")) }, 4, 0)
            viewer.load()
            assertFalse(viewer.state.value.mayEdit)
            assertFalse(viewer.state.value.showSaveBar)
            viewer.setName("x")
            assertEquals("Mine", viewer.state.value.form.name)
            val noNew = SettingsPickRuleEditPresenter(server.api(), { testAccount(Role.ADMIN, listOf("manage_library")) }, 0, 0)
            noNew.load()
            assertTrue(noNew.state.value.needsPermission)
        }

    @Test
    fun trashProfilesApplyAndSyncAgain() =
        runTest {
            val server = editServer()
            server.json(
                "GET",
                "/api/v1/trash/profiles",
                """{"profiles":[{"id":"p0","app":"radarr","name":"Other","description":"d0"},{"id":"p1","app":"radarr","name":"HD","description":"hd"}]}""",
            )
            server.on("POST", "/api/v1/pick-rules/4/trash") {
                respondJson(
                    """{"pick_rules":$detail,"added":2,"updated":1,"scored":[{"key":"own:20","name":"a","points":5},{"key":"own:21","name":"b","points":6}],
                    "skipped":[{"name":"c","kinds":["language"],"reason":"x"},{"name":"d","reason":"too odd"}]}""",
                )
            }
            val presenter = SettingsPickRuleEditPresenter(server.api(), owner, 4, 0)
            presenter.load()
            presenter.setTrashApp("radarr")
            var t = presenter.state.value.trash
            assertEquals(listOf("p0", "p1"), t.profiles.map { it.id })
            assertEquals("p1", t.pick)
            assertEquals("hd", t.choice!!.description)
            assertEquals("app=radarr", server.request("GET", "/api/v1/trash/profiles").query)
            presenter.setName("Dirty")
            presenter.applyTrash()
            assertFalse(presenter.state.value.trash.busy)
            assertTrue(server.requests.none { it.path.endsWith("/trash") })
            presenter.discard()
            presenter.setTrashPick("p0")
            presenter.applyTrash()
            t = presenter.state.value.trash
            assertEquals(2, t.result!!.added)
            assertEquals(2, t.result!!.scored)
            assertEquals(listOf("language"), t.result!!.skipped.first().kinds)
            assertEquals("applied", presenter.state.value.notice!!.kind)
            assertEquals("HD", presenter.state.value.notice!!.name)
            assertTrue(server.request("POST", "/api/v1/pick-rules/4/trash").body.orEmpty().contains(""""profile_id":"p0""""))
            presenter.syncAgain()
            assertTrue(server.request("POST", "/api/v1/pick-rules/4/trash").body.orEmpty().contains(""""profile_id":"p1""""))
        }
}
