package org.http4k.connect.google.analytics

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.CapturingHttpHandler
import org.http4k.connect.google.analytics.action.CLIENT_ID
import org.http4k.connect.google.analytics.action.DOCUMENT_HOST
import org.http4k.connect.google.analytics.action.DOCUMENT_PATH
import org.http4k.connect.google.analytics.action.DOCUMENT_TITLE
import org.http4k.connect.google.analytics.action.MEASUREMENT_ID
import org.http4k.connect.google.analytics.action.VERSION
import org.http4k.connect.google.model.ClientId
import org.http4k.connect.google.model.TrackingId
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.core.Status.Companion.CONTINUE
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.UriTemplate
import org.http4k.core.body.form
import org.http4k.core.then
import org.http4k.filter.DEFAULT_USER_AGENT
import org.http4k.filter.LogPageView
import org.http4k.filter.ServerFilters
import org.http4k.hamkrest.hasStatus
import org.http4k.routing.RoutedRequest
import org.junit.jupiter.api.Test

class GoogleAnalyticsTest {
    private val testHttpClient = CapturingHttpHandler()
    private val client = GoogleAnalytics.Http(testHttpClient)
    private val analytics =
        ServerFilters.LogPageView(client, TrackingId.of("TEST-MEASUREMENT-ID")) { ClientId.of("TEST-CLIENT-ID") }.then {
            when {
                it.uri.path.contains("fail") -> Response(BAD_REQUEST)
                it.uri.path.contains("informational") -> Response(CONTINUE)
                it.uri.path.contains("redirect") -> Response(SEE_OTHER)
                else -> Response(OK)
            }
        }

    @Test
    fun `logs request as page view`() {
        val response = analytics(Request(GET, "https://www.http4k.org/some/world"))

        assertThat(response, hasStatus(OK))
        assertPageView("/some/world", "/some/world", "www.http4k.org")
    }

    @Test
    fun `logs routed request as page view`() {
        val response = analytics(RoutedRequest(Request(GET, "/some/world"), UriTemplate.from("/some/{hello}")))

        assertThat(response, hasStatus(OK))
        assertPageView("some/{hello}", "some/{hello}", "")
    }

    @Test
    fun `logs request with host as page view`() {
        val response = analytics(Request(GET, "/some/world").header("host", "www.http4k.org"))

        assertThat(response, hasStatus(OK))
        assertPageView("/some/world", "/some/world", "www.http4k.org")
    }

    @Test
    fun `logs page view for informational response`() {
        val response = analytics(Request(GET, "/informational"))

        assertThat(response, hasStatus(CONTINUE))
        assertPageView("/informational", "/informational", "")
    }

    @Test
    fun `logs page view for redirect response`() {
        val response = analytics(Request(GET, "/redirect"))

        assertThat(response, hasStatus(SEE_OTHER))
        assertPageView("/redirect", "/redirect", "")
    }

    @Test
    fun `ignore bad responses from google analytics`() {
        testHttpClient.response = Response(CLIENT_TIMEOUT)

        val response = analytics(Request(GET, "some/world"))

        assertThat(response, hasStatus(OK))
        assertPageView("some/world", "some/world", "")
    }

    @Test
    fun `don't log page view on unsuccessful response`() {
        val response = analytics(Request(GET, "/fail"))

        assertThat(response, hasStatus(BAD_REQUEST))
        assertNoPageView()
    }

    private fun assertPageView(title: String, path: String, host: String) {
        assertThat(
            testHttpClient.captured, equalTo(
                Request(POST, "/collect")
                    .header("User-Agent", DEFAULT_USER_AGENT)
                    .form(VERSION, "1")
                    .form(MEASUREMENT_ID, "TEST-MEASUREMENT-ID")
                    .form(CLIENT_ID, "TEST-CLIENT-ID")
                    .form(DOCUMENT_TITLE, title)
                    .form(DOCUMENT_PATH, path)
                    .form(DOCUMENT_HOST, host)
            )
        )
    }

    private fun assertNoPageView() {
        assertThat(testHttpClient.captured, absent())
    }
}
