/**
 * JVM harness: CushionBridgePlanParser + threshold bridge gating.
 * Fixture: src/test/resources/mock_cushion_bridge.json
 * Run: cd android && ./gradlew testDebugUnitTest --tests com.narrativedj.app.radio.CushionBridgePlanParserTest
 */
package com.narrativedj.app.radio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class CushionBridgePlanParserTest {

    @Test
    fun parses_fixture_andKeepsBridgesBelowThreshold() {
        val plan = CushionBridgePlanParser.parseJson(readResource("mock_cushion_bridge.json"))
        assertNotNull(plan)
        assertEquals("Hotel California Eagles", plan!!.selectedSearchQuery)
        assertEquals(0.42, plan.similarity, 0.001)
        assertEquals(2, plan.bridgesForPlayback().size)
        assertTrue(plan.similarity < CushionBridgePlan.SIMILARITY_THRESHOLD)
    }

    @Test
    fun highSimilarity_clearsBridgesForDirectPlay() {
        val plan = CushionBridgePlan(
            selectedSearchQuery = "California Dreamin'",
            similarity = 0.8,
            bridgeSearchQueries = listOf("should not play"),
        )
        assertTrue(plan.bridgesForPlayback().isEmpty())
    }

    @Test
    fun rejects_invalidSimilarity() {
        assertEquals(
            null,
            CushionBridgePlanParser.parseJson(
                """{"selected_search_query":"x","similarity":1.5,"bridge_search_queries":[]}""",
            ),
        )
    }

    private fun readResource(name: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream(name)
            ?: error("Missing test resource: $name")
        return stream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
    }
}
