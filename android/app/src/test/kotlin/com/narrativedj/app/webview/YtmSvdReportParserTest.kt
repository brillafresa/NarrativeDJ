package com.narrativedj.app.webview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YtmSvdReportParserTest {

    @Test
    fun parse_healthyReport_parsesAllFields() {
        val json = """
            {
              "healthy": true,
              "fields": {
                "title": {"field":"title","selector":"#poc-track-title","ok":true,"index":2},
                "artist": {"field":"artist","selector":"#poc-track-artist","ok":true,"index":2},
                "playButton": {"field":"playButton","selector":"#poc-play-button","ok":true,"index":2}
              }
            }
        """.trimIndent()

        val report = requireNotNull(YtmSvdReportParser.parse(json))

        assertTrue(report.healthy)
        assertEquals(3, report.fields.size)
        assertEquals("#poc-track-title", report.fields["title"]?.selector)
    }

    @Test
    fun parse_unhealthyReport_setsHealthyFalse() {
        val json = """
            {
              "healthy": false,
              "fields": {
                "title": {"field":"title","selector":null,"ok":false,"index":-1}
              }
            }
        """.trimIndent()

        val report = requireNotNull(YtmSvdReportParser.parse(json))
        assertFalse(report.healthy)
        assertFalse(report.fields["title"]!!.ok)
    }

    @Test
    fun parse_invalidJson_returnsNull() {
        assertEquals(null, YtmSvdReportParser.parse("not-json"))
    }
}
