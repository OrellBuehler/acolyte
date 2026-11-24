package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.model.SettingsServerAlert
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerAlertField
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerAlertForm
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerAlertKind
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerAlertsNoticeKind
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerAlertsPresenter
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerMissing
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerTestKind
import ch.orellbuehler.acolyte.feature.settings.server.missingFor
import ch.orellbuehler.acolyte.session.Role
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsServerAlertsTest {
    private val mask = "••••••••"

    private fun alert(
        kind: String,
        build: SettingsServerAlert.() -> SettingsServerAlert = { this },
    ): SettingsServerAlert = SettingsServerAlert(id = 1, name = "Phone", kind = kind, enabled = true, moments = listOf("grab")).build()

    @Test
    fun missingFieldsFollowTheWebRules() {
        val cases =
            listOf(
                alert("telegram") to SettingsServerMissing.BOT_AND_CHAT,
                alert("telegram") { copy(chatId = "@news") } to SettingsServerMissing.BOT,
                alert("telegram") { copy(botToken = mask) } to SettingsServerMissing.CHAT,
                alert("telegram") { copy(chatId = "@news", botToken = mask) } to SettingsServerMissing.NOTHING,
                alert("ntfy") { copy(serverUrl = "https://ntfy.example.com") } to SettingsServerMissing.TOPIC,
                alert("ntfy") { copy(topic = "tv") } to SettingsServerMissing.NOTHING,
                alert("gotify") { copy(serverUrl = "https://gotify.example.com") } to SettingsServerMissing.APP,
                alert("gotify") { copy(appToken = mask) } to SettingsServerMissing.SERVER,
                alert("gotify") to SettingsServerMissing.SERVER_AND_APP,
                alert("gotify") { copy(serverUrl = "https://g", appToken = mask) } to SettingsServerMissing.NOTHING,
                alert("pushover") to SettingsServerMissing.USER_AND_APP,
                alert("pushover") { copy(userKey = "u") } to SettingsServerMissing.APP,
                alert("pushover") { copy(appToken = mask) } to SettingsServerMissing.USER,
                alert("pushover") { copy(appToken = mask, userKey = "u") } to SettingsServerMissing.NOTHING,
                alert("discord") to SettingsServerMissing.LINK,
                alert("discord") { copy(url = mask) } to SettingsServerMissing.NOTHING,
                alert("webhook") to SettingsServerMissing.ADDRESS,
                alert("webhook") { copy(url = mask) } to SettingsServerMissing.NOTHING,
                alert("email") to SettingsServerMissing.NOTHING,
            )
        cases.forEach { (a, want) -> assertEquals(want, missingFor(a), "${a.kind} ${a.chatId} ${a.url}") }
    }

    @Test
    fun changingAnAlertLeavesEverySecretEmptyAndRemembersWhichAreStored() {
        val form =
            SettingsServerAlertForm.from(
                alert("gotify") { copy(serverUrl = "https://gotify.example.com", appToken = mask, allowLocalNetwork = true) },
            )
        assertEquals("", form.appToken)
        assertEquals("", form.botToken)
        assertTrue(form.appTokenStored)
        assertFalse(form.botTokenStored)
        assertEquals("https://gotify.example.com", form.serverUrl)
        assertEquals(listOf("grab"), form.picked)
        assertTrue(form.allowLocalNetwork)
        assertFalse(form.isNew)
        assertEquals("587", form.smtpPort)
        assertEquals("starttls", form.smtpSecurity)
        assertEquals("sign", form.webhookAuth)
    }

    @Test
    fun aNewFormStartsWithEveryMomentAndSendingOn() {
        val form = SettingsServerAlertForm.blank()
        assertTrue(form.isNew)
        assertTrue(form.enabled)
        assertFalse(form.allowLocalNetwork)
        assertEquals(18, form.picked.size)
        assertEquals(listOf("requests", "library"), form.groups.map { it.key })
        assertEquals(8, form.groups[0].moments.size)
        assertEquals(10, form.groups[1].moments.size)
        assertTrue(form.groups.all { it.allOn })
    }

    @Test
    fun theKindPickerEmptiesTheSharedTokenAndServer() {
        val typed =
            SettingsServerAlertForm
                .blank()
                .withKind(SettingsServerAlertKind.GOTIFY)
                .withText(SettingsServerAlertField.SERVER_URL, "https://gotify.example.com")
                .withText(SettingsServerAlertField.APP_TOKEN, "abc")
                .withText(SettingsServerAlertField.TOPIC, "tv")
        val next = typed.withKind(SettingsServerAlertKind.NTFY)
        assertEquals("", next.appToken)
        assertEquals("", next.serverUrl)
        assertEquals("tv", next.topic)
        assertEquals(typed, typed.withKind(SettingsServerAlertKind.GOTIFY))
    }

    @Test
    fun fieldsAreFilteredAndLimited() {
        var form = SettingsServerAlertForm.blank()
        form = form.withText(SettingsServerAlertField.TOPIC, "my topic!_1-x")
        assertEquals("mytopic_1-x", form.topic)
        form = form.withText(SettingsServerAlertField.TOPIC, "a".repeat(80))
        assertEquals(64, form.topic.length)
        form = form.withText(SettingsServerAlertField.ALERT_NAME, "n".repeat(120))
        assertEquals(80, form.name.length)
        form = form.withText(SettingsServerAlertField.SMTP_PORT, "58a7x")
        assertEquals("587", form.smtpPort)
        assertEquals("587", form.withText(SettingsServerAlertField.SMTP_PORT, "5870000").smtpPort.take(3))
        assertEquals(5, form.withText(SettingsServerAlertField.SMTP_PORT, "5870000").smtpPort.length)
    }

    @Test
    fun saveNeedsANameThePortAndWhatAKindRequiresWhenAdding() {
        var form = SettingsServerAlertForm.blank()
        assertFalse(form.canSave)
        form = form.withText(SettingsServerAlertField.ALERT_NAME, "  ")
        assertFalse(form.canSave)
        form = form.withText(SettingsServerAlertField.ALERT_NAME, "Phone")
        assertFalse(form.canSave)
        form = form.withText(SettingsServerAlertField.URL, "https://discord.com/api/webhooks/1")
        assertTrue(form.canSave)
        form = form.withKind(SettingsServerAlertKind.EMAIL).withText(SettingsServerAlertField.SMTP_HOST, "smtp.example.com")
        assertFalse(form.canSave)
        form = form.withText(SettingsServerAlertField.SMTP_FROM, "a@example.com")
        assertTrue(form.canSave)
        assertFalse(form.withText(SettingsServerAlertField.SMTP_PORT, "70000").canSave)
        assertFalse(form.withText(SettingsServerAlertField.SMTP_PORT, "0").canSave)
        assertFalse(form.withText(SettingsServerAlertField.SMTP_PORT, "").canSave)
        assertTrue(form.withText(SettingsServerAlertField.SMTP_PORT, "465").canSave)
        val gotify = form.withKind(SettingsServerAlertKind.GOTIFY).withText(SettingsServerAlertField.SERVER_URL, "https://g")
        assertFalse(gotify.canSave)
        assertTrue(gotify.withText(SettingsServerAlertField.APP_TOKEN, "tok").canSave)
        val ntfy = form.withKind(SettingsServerAlertKind.NTFY)
        assertFalse(ntfy.canSave)
        assertTrue(ntfy.withText(SettingsServerAlertField.TOPIC, "tv").canSave)
        val editing = SettingsServerAlertForm.from(alert("gotify") { copy(appToken = mask) })
        assertTrue(editing.canSave)
    }

    @Test
    fun momentGroupsToggleTogether() {
        var form = SettingsServerAlertForm.blank()
        form = form.withGroup("requests", false)
        assertTrue(form.groups[0].moments.none { it.on })
        assertTrue(form.groups[1].allOn)
        assertEquals(10, form.picked.size)
        form = form.withMoment("ask_waiting", true)
        assertEquals(
            listOf("ask_waiting"),
            form.groups[0]
                .moments
                .filter { it.on }
                .map { it.id },
        )
        form = form.withGroup("requests", true)
        assertTrue(form.groups[0].allOn)
        form = form.withGroup("library", false).withMoment("grab", true)
        assertEquals(
            listOf("grab"),
            form.groups[1]
                .moments
                .filter { it.on }
                .map { it.id },
        )
        assertEquals(form.picked.distinct(), form.picked)
    }

    @Test
    fun theMomentsAnAlertKeepsOutsideTheFormSurvive() {
        val form = SettingsServerAlertForm.from(alert("discord") { copy(moments = listOf("subtitle", "grab")) })
        val next = form.withGroup("library", false)
        assertEquals(listOf("subtitle"), next.picked)
    }

    @Test
    fun theBodyHoldsOnlyTheKindsFields() {
        val telegram =
            SettingsServerAlertForm
                .blank()
                .withKind(SettingsServerAlertKind.TELEGRAM)
                .withText(SettingsServerAlertField.ALERT_NAME, " Phone ")
                .withText(SettingsServerAlertField.BOT_TOKEN, " 123:AAH ")
                .withText(SettingsServerAlertField.CHAT_ID, "-100123")
                .toInput()
        assertEquals("Phone", telegram.name)
        assertEquals("telegram", telegram.kind)
        assertEquals("123:AAH", telegram.botToken)
        assertEquals("-100123", telegram.chatId)
        assertNull(telegram.url)
        assertNull(telegram.smtpHost)
        assertNull(telegram.clearAppToken)
        assertEquals(18, telegram.moments?.size)
        assertEquals(true, telegram.enabled)
        assertEquals(false, telegram.allowLocalNetwork)
        val ntfy =
            SettingsServerAlertForm
                .from(alert("ntfy") { copy(topic = "tv", appToken = mask, serverUrl = "https://ntfy.example.com") })
                .withClearToken(true)
                .toInput()
        assertEquals("tv", ntfy.topic)
        assertEquals("", ntfy.appToken)
        assertEquals(true, ntfy.clearAppToken)
        assertEquals("https://ntfy.example.com", ntfy.serverUrl)
        val email =
            SettingsServerAlertForm
                .blank()
                .withKind(SettingsServerAlertKind.EMAIL)
                .withText(SettingsServerAlertField.SMTP_HOST, "smtp.example.com")
                .withText(SettingsServerAlertField.SMTP_FROM, "a@example.com")
                .withText(SettingsServerAlertField.SMTP_SECURITY, "tls")
                .withText(SettingsServerAlertField.SMTP_PORT, "465")
                .toInput()
        assertEquals(465, email.smtpPort)
        assertEquals("tls", email.smtpSecurity)
        assertEquals("", email.smtpPass)
        assertNull(email.url)
        val webhook =
            SettingsServerAlertForm
                .blank()
                .withKind(SettingsServerAlertKind.WEBHOOK)
                .withText(SettingsServerAlertField.URL, "https://example.com/holyarrs")
                .withText(SettingsServerAlertField.WEBHOOK_AUTH, "bearer")
                .toInput()
        assertEquals("bearer", webhook.webhookAuth)
        assertEquals("", webhook.secret)
        assertNull(webhook.smtpPort)
    }

    @Test
    fun clearingTheNtfyTokenEmptiesTheFieldAndTypingOneUndoesIt() {
        val form = SettingsServerAlertForm.from(alert("ntfy") { copy(topic = "tv", appToken = mask) })
        assertTrue(form.canClearToken)
        val cleared = form.withText(SettingsServerAlertField.APP_TOKEN, "tk_x").withClearToken(true)
        assertEquals("", cleared.appToken)
        assertTrue(cleared.clearAppToken)
        assertFalse(cleared.withText(SettingsServerAlertField.APP_TOKEN, "tk_y").clearAppToken)
        assertFalse(SettingsServerAlertForm.blank().withKind(SettingsServerAlertKind.NTFY).canClearToken)
    }

    private val listJson =
        """{"alerts":[
            {"id":1,"name":"Phone","kind":"telegram","enabled":true,"moments":["grab","import"],
              "bot_token":"$mask","chat_id":"@news","allow_local_network":false,"last_sent_at":"2026-11-01T10:00:00Z"},
            {"id":2,"name":"Tablet","kind":"gotify","enabled":false,"moments":[],"server_url":"https://gotify.example.com",
              "allow_local_network":true,"last_error":"the server refused"},
            {"id":3,"name":"Mail","kind":"email","enabled":true,"moments":["health"],"smtp_host":"smtp.example.com","smtp_port":465,
              "smtp_security":"tls","smtp_from":"a@example.com","smtp_pass":"$mask","staff_to":"b@example.com","allow_local_network":false}
            ],"moments":["ask_waiting","grab"]}"""

    private fun server(): FakeServer {
        val server = FakeServer()
        server.json("GET", "/api/v1/alerts", listJson)
        server.json("GET", "/api/v1/settings/alerts", """{"pack_alerts":"one"}""")
        return server
    }

    private fun presenter(
        server: FakeServer,
        role: Role = Role.OWNER,
    ) = SettingsServerAlertsPresenter(server.api()) { testAccount(role, emptyList()) }

    @Test
    fun loadsTheAlertsAndThePackStyle() =
        runTest {
            val presenter = presenter(server())
            presenter.load()
            val s = presenter.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertEquals(listOf("Phone", "Tablet", "Mail"), s.alerts.map { it.name })
            assertEquals(
                listOf(SettingsServerMissing.NOTHING, SettingsServerMissing.APP, SettingsServerMissing.NOTHING),
                s.alerts.map { it.missing },
            )
            assertEquals("2026-11-01T10:00:00Z", s.alerts[0].lastSentAt)
            assertEquals("the server refused", s.alerts[1].lastError)
            assertFalse(s.alerts[1].enabled)
            assertEquals("one", s.pack.style)
            assertFalse(s.pack.loading)
            assertTrue(s.showPack)
            assertFalse(s.isEmpty)
        }

    @Test
    fun peopleWithoutTheSettingsPermissionGetNothing() =
        runTest {
            val server = server()
            val presenter = SettingsServerAlertsPresenter(server.api()) { testAccount(Role.ADMIN, listOf("approve")) }
            presenter.load()
            assertFalse(presenter.state.value.allowed)
            assertFalse(presenter.state.value.showPack)
            assertEquals(0, server.recorded().size)
        }

    @Test
    fun anEmptyListIsEmpty() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/alerts", """{"alerts":[],"moments":[]}""")
            server.json("GET", "/api/v1/settings/alerts", """{"pack_alerts":"each","each_limit":25}""")
            val presenter = presenter(server)
            presenter.load()
            assertTrue(presenter.state.value.isEmpty)
            assertEquals("each", presenter.state.value.pack.style)
        }

    @Test
    fun aFailedListKeepsThePackCardUnlessBothFailed() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/alerts", """{"error":"boom"}""", 500)
            server.json("GET", "/api/v1/settings/alerts", """{"pack_alerts":"one"}""")
            val presenter = presenter(server)
            presenter.load()
            assertEquals(Phase.FAILED, presenter.state.value.status.phase)
            assertTrue(presenter.state.value.showPack)
            server.json("GET", "/api/v1/settings/alerts", """{"error":"boom"}""", 500)
            presenter.refresh()
            assertFalse(presenter.state.value.showPack)
            assertNotNull(presenter.state.value.pack.error)
            server.json("GET", "/api/v1/settings/alerts", """{"pack_alerts":"each"}""")
            presenter.retryPack()
            assertEquals("each", presenter.state.value.pack.style)
            assertNull(presenter.state.value.pack.error)
        }

    @Test
    fun addsAnAlert() =
        runTest {
            val server = server()
            server.on(
                "POST",
                "/api/v1/alerts",
            ) { respondJson("""{"id":9,"name":"Hook","kind":"discord","enabled":true,"moments":[]}""", 201) }
            val presenter = presenter(server)
            presenter.load()
            presenter.startAdd()
            presenter.setText(SettingsServerAlertField.ALERT_NAME, "Hook")
            presenter.setText(SettingsServerAlertField.URL, "https://discord.com/api/webhooks/1/x")
            presenter.setGroup("library", false)
            assertTrue(
                presenter.state.value.form!!
                    .canSave,
            )
            presenter.save()
            val s = presenter.state.value
            assertNull(s.form)
            assertEquals(SettingsServerAlertsNoticeKind.SAVED, s.notice?.kind)
            assertEquals("Hook", s.notice?.name)
            val body = server.request("POST", "/api/v1/alerts").body.orEmpty()
            assertTrue(body.contains(""""name":"Hook""""), body)
            assertTrue(body.contains(""""kind":"discord""""), body)
            assertTrue(body.contains(""""url":"https://discord.com/api/webhooks/1/x""""), body)
            assertTrue(body.contains(""""moments":["ask_waiting""""), body)
            assertFalse(body.contains("grab"), body)
            assertFalse(body.contains("smtp"), body)
            assertEquals(2, server.recorded().count { it.method == "GET" && it.path == "/api/v1/alerts" })
        }

    @Test
    fun changesAnAlertKeepingItsStoredSecret() =
        runTest {
            val server = server()
            server.on("PUT", "/api/v1/alerts/1") { respondJson((it.body as TextContent).text) }
            val presenter = presenter(server)
            presenter.load()
            presenter.startChange(1)
            val opened = presenter.state.value.form
            assertEquals(1L, opened?.id)
            assertEquals(SettingsServerAlertKind.TELEGRAM, opened?.kind)
            assertTrue(opened?.botTokenStored == true)
            assertEquals("@news", opened?.chatId)
            presenter.setText(SettingsServerAlertField.CHAT_ID, "-100123")
            presenter.setEnabled(false)
            presenter.setAllowLocalNetwork(true)
            presenter.setKind(SettingsServerAlertKind.WEBHOOK)
            assertEquals(
                SettingsServerAlertKind.TELEGRAM,
                presenter.state.value.form
                    ?.kind,
            )
            presenter.save()
            assertNull(presenter.state.value.form)
            val body = server.request("PUT", "/api/v1/alerts/1").body.orEmpty()
            assertTrue(body.contains(""""bot_token":"""""), body)
            assertTrue(body.contains(""""chat_id":"-100123""""), body)
            assertTrue(body.contains(""""enabled":false"""), body)
            assertTrue(body.contains(""""allow_local_network":true"""), body)
            assertTrue(body.contains(""""moments":["grab","import"]"""), body)
        }

    @Test
    fun aRefusedSaveKeepsTheFormAndShowsTheServersWords() =
        runTest {
            val server = server()
            server.json("POST", "/api/v1/alerts", """{"error":"another Alert has that name"}""", 409)
            val presenter = presenter(server)
            presenter.load()
            presenter.startAdd()
            presenter.setText(SettingsServerAlertField.ALERT_NAME, "Phone")
            presenter.setText(SettingsServerAlertField.URL, "https://discord.com/api/webhooks/1/x")
            presenter.save()
            val form = presenter.state.value.form
            assertNotNull(form)
            assertFalse(form.saving)
            assertEquals(ApiErrorCode.CONFLICT, form.error?.code)
            assertEquals("another Alert has that name", form.error?.message)
            assertNull(presenter.state.value.notice)
            presenter.cancelForm()
            assertNull(presenter.state.value.form)
        }

    @Test
    fun saveDoesNothingWhileTheFormIsInvalid() =
        runTest {
            val server = server()
            val presenter = presenter(server)
            presenter.load()
            presenter.startAdd()
            presenter.save()
            assertTrue(server.recorded().none { it.method == "POST" })
        }

    @Test
    fun aTestOfAnAlertThatStillNeedsThingsIsRefusedHere() =
        runTest {
            val server = server()
            val presenter = presenter(server)
            presenter.load()
            presenter.test(2)
            val row =
                presenter.state.value.alerts
                    .first { it.id == 2L }
            assertEquals(SettingsServerTestKind.TYPE_FIRST, row.test?.kind)
            assertTrue(server.recorded().none { it.method == "POST" })
        }

    @Test
    fun aTestShowsSentOrTheFailureAndReloads() =
        runTest {
            val server = server()
            server.json("POST", "/api/v1/alerts/1/test", """{"sent":true}""")
            server.json("POST", "/api/v1/alerts/3/test", """{"error":"the mail server refused the test"}""", 502)
            val presenter = presenter(server)
            presenter.load()
            presenter.test(1)
            var s = presenter.state.value
            assertEquals(
                SettingsServerTestKind.SENT,
                s.alerts
                    .first { it.id == 1L }
                    .test
                    ?.kind,
            )
            assertFalse(s.alerts.first { it.id == 1L }.testing)
            presenter.test(3)
            s = presenter.state.value
            val failed = s.alerts.first { it.id == 3L }.test
            assertEquals(SettingsServerTestKind.FAILED, failed?.kind)
            assertEquals(502, failed?.error?.status)
            assertTrue(
                failed
                    ?.error
                    ?.message
                    .orEmpty()
                    .contains("refused"),
            )
            assertEquals(
                SettingsServerTestKind.SENT,
                s.alerts
                    .first { it.id == 1L }
                    .test
                    ?.kind,
            )
            assertEquals(3, server.recorded().count { it.method == "GET" && it.path == "/api/v1/alerts" })
        }

    @Test
    fun removesAnAlertOrReportsTheFailure() =
        runTest {
            val server = server()
            server.on("DELETE", "/api/v1/alerts/1") { respondJson("", 204) }
            server.json("DELETE", "/api/v1/alerts/2", """{"error":"no such Alert"}""", 404)
            val presenter = presenter(server)
            presenter.load()
            server.json(
                "GET",
                "/api/v1/alerts",
                """{"alerts":[{"id":2,"name":"Tablet","kind":"gotify","enabled":false,"moments":[]}],"moments":[]}""",
            )
            presenter.remove(1)
            assertEquals(
                listOf(2L),
                presenter.state.value.alerts
                    .map { it.id },
            )
            presenter.remove(2)
            val notice = presenter.state.value.notice
            assertEquals(SettingsServerAlertsNoticeKind.REMOVE_FAILED, notice?.kind)
            assertEquals(ApiErrorCode.NOT_FOUND, notice?.error?.code)
            presenter.dismissNotice(notice!!.seq)
            assertNull(presenter.state.value.notice)
        }

    @Test
    fun savesThePackStyle() =
        runTest {
            val server = server()
            server.on("PUT", "/api/v1/settings/alerts") { respondJson((it.body as TextContent).text) }
            val presenter = presenter(server)
            presenter.load()
            presenter.setPack("one")
            assertTrue(server.recorded().none { it.method == "PUT" })
            presenter.setPack("each")
            val s = presenter.state.value
            assertEquals("each", s.pack.style)
            assertFalse(s.pack.saving)
            assertEquals(SettingsServerAlertsNoticeKind.PACK_SAVED, s.notice?.kind)
            assertEquals("each", s.notice?.name)
            assertEquals("""{"pack_alerts":"each"}""", server.request("PUT", "/api/v1/settings/alerts").body)
        }

    @Test
    fun aFailedPackSaveKeepsTheOldStyle() =
        runTest {
            val server = server()
            server.json("PUT", "/api/v1/settings/alerts", """{"error":"pack_alerts is not one or each"}""", 400)
            val presenter = presenter(server)
            presenter.load()
            presenter.setPack("each")
            val s = presenter.state.value
            assertEquals("one", s.pack.style)
            assertEquals(SettingsServerAlertsNoticeKind.PACK_FAILED, s.notice?.kind)
            assertEquals(ApiErrorCode.BAD_REQUEST, s.notice?.error?.code)
        }
}
