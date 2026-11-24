package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.NewReport
import ch.orellbuehler.acolyte.api.model.ReportAnswer
import ch.orellbuehler.acolyte.api.model.ReportFix
import ch.orellbuehler.acolyte.api.model.ReportList
import ch.orellbuehler.acolyte.api.model.TitleSearchBody
import ch.orellbuehler.acolyte.api.model.TitleSearchQueued

internal data object ReportsEndpoints : EndpointGroup() {
    val list = get("/api/v1/reports", ReportList.serializer(), Need.SIGNED_IN, query = listOf("state", "limit"))
    val make = post("/api/v1/reports", NewReport.serializer(), ReportAnswer.serializer(), Need.SIGNED_IN)
    val fix = post("/api/v1/reports/{id}/fix", ReportFix.serializer(), ReportAnswer.serializer(), Need.MANAGE_LIBRARY)
    val reopen = post("/api/v1/reports/{id}/reopen", ReportAnswer.serializer(), Need.MANAGE_LIBRARY)
    val search =
        post("/api/v1/titles/{id}/search", TitleSearchBody.serializer(), TitleSearchQueued.serializer(), Need.MANAGE_LIBRARY, Part.TITLE)
}
