package com.narrativedj.app.b2b

import android.content.Context
import com.narrativedj.app.R

enum class GuardMessageKey {
    PERSONAL_OK,
    COMMERCIAL_B2B_ACTIVE,
    COMMERCIAL_BLOCKED,
}

data class CommercialGuardResult(
    val allowed: Boolean,
    val requiresB2bLicense: Boolean,
    val messageKey: GuardMessageKey,
)

fun CommercialGuardResult.localizedMessage(context: Context): String {
    return when (messageKey) {
        GuardMessageKey.PERSONAL_OK -> context.getString(R.string.guard_personal_ok)
        GuardMessageKey.COMMERCIAL_B2B_ACTIVE -> context.getString(R.string.guard_commercial_b2b)
        GuardMessageKey.COMMERCIAL_BLOCKED -> context.getString(R.string.guard_commercial_blocked)
    }
}

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
                messageKey = GuardMessageKey.PERSONAL_OK,
            )
        }
        if (providerMode == MusicProviderMode.B2B_STREAMING && hasValidB2bLicense) {
            return CommercialGuardResult(
                allowed = true,
                requiresB2bLicense = true,
                messageKey = GuardMessageKey.COMMERCIAL_B2B_ACTIVE,
            )
        }
        return CommercialGuardResult(
            allowed = false,
            requiresB2bLicense = true,
            messageKey = GuardMessageKey.COMMERCIAL_BLOCKED,
        )
    }
}

fun MusicProvider.localizedLabel(context: Context): String {
    return when (mode) {
        MusicProviderMode.BYOK_WEBVIEW -> context.getString(R.string.provider_byok)
        MusicProviderMode.B2B_STREAMING -> context.getString(R.string.provider_b2b)
    }
}
