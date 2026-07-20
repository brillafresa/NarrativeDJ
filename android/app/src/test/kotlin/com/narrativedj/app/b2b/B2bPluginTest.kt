package com.narrativedj.app.b2b

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicProviderFactoryTest {

    @Test
    fun byokProvider_doesNotResolveStreamUrl() = runBlocking {
        val provider = MusicProviderFactory.create(
            MusicProviderMode.BYOK_WEBVIEW,
            B2bPartnerApiClient(null, null),
        )
        assertNull(provider.resolveStreamUrl("california_dreamin"))
        assertTrue(provider.playbackSourceLabel().contains("BYOK"))
    }

    @Test
    fun b2bProvider_usesMockStreamWhenNoPartner() = runBlocking {
        val provider = MusicProviderFactory.create(
            MusicProviderMode.B2B_STREAMING,
            B2bPartnerApiClient(null, "b2b-test-key"),
        )
        val url = provider.resolveStreamUrl("california_dreamin")
        assertNotNull(url)
        assertTrue(url!!.contains("partner.mock"))
    }
}

class CommercialSpaceGuardTest {

    @Test
    fun personalVenue_alwaysAllowed() {
        val result = CommercialSpaceGuard.evaluate(
            CommercialSpaceGuard.VenueType.PERSONAL,
            MusicProviderMode.BYOK_WEBVIEW,
            hasValidB2bLicense = false,
        )
        assertTrue(result.allowed)
        assertFalse(result.requiresB2bLicense)
    }

    @Test
    fun commercialVenue_withoutB2bLicense_blocked() {
        val result = CommercialSpaceGuard.evaluate(
            CommercialSpaceGuard.VenueType.COMMERCIAL,
            MusicProviderMode.BYOK_WEBVIEW,
            hasValidB2bLicense = false,
        )
        assertFalse(result.allowed)
        assertTrue(result.requiresB2bLicense)
    }

    @Test
    fun commercialVenue_withB2bLicense_allowed() {
        val result = CommercialSpaceGuard.evaluate(
            CommercialSpaceGuard.VenueType.COMMERCIAL,
            MusicProviderMode.B2B_STREAMING,
            hasValidB2bLicense = true,
        )
        assertTrue(result.allowed)
    }
}
