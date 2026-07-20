package com.narrativedj.app.b2b

data class CommercialGuardResult(
    val allowed: Boolean,
    val requiresB2bLicense: Boolean,
    val message: String,
)

/**
 * GPS/commercial-space guard — encourages B2B license in commercial contexts (Phase 3).
 */
object CommercialSpaceGuard {
    enum class VenueType {
        PERSONAL,
        COMMERCIAL,
    }

    fun evaluate(
        venueType: VenueType,
        providerMode: MusicProviderMode,
        hasValidB2bLicense: Boolean,
    ): CommercialGuardResult {
        if (venueType == VenueType.PERSONAL) {
            return CommercialGuardResult(
                allowed = true,
                requiresB2bLicense = false,
                message = "Personal use — BYOK mode OK",
            )
        }
        if (providerMode == MusicProviderMode.B2B_STREAMING && hasValidB2bLicense) {
            return CommercialGuardResult(
                allowed = true,
                requiresB2bLicense = true,
                message = "Commercial space — B2B license active",
            )
        }
        return CommercialGuardResult(
            allowed = false,
            requiresB2bLicense = true,
            message = "Commercial space detected — configure B2B license or switch venue to personal",
        )
    }
}
