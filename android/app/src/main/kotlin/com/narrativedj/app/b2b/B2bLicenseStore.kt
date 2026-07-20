package com.narrativedj.app.b2b

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class B2bLicenseStore(context: Context) {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun saveLicenseKey(licenseKey: String) {
        val trimmed = licenseKey.trim()
        if (trimmed.isEmpty()) {
            prefs.edit().remove(KEY_LICENSE).apply()
        } else {
            prefs.edit().putString(KEY_LICENSE, trimmed).apply()
        }
    }

    fun getLicenseKey(): String? = prefs.getString(KEY_LICENSE, null)?.takeIf { it.isNotBlank() }

    fun savePartnerBaseUrl(baseUrl: String) {
        val trimmed = baseUrl.trim()
        if (trimmed.isEmpty()) {
            prefs.edit().remove(KEY_BASE_URL).apply()
        } else {
            prefs.edit().putString(KEY_BASE_URL, trimmed).apply()
        }
    }

    fun getPartnerBaseUrl(): String? = prefs.getString(KEY_BASE_URL, null)?.takeIf { it.isNotBlank() }

    fun saveProviderMode(mode: MusicProviderMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }

    fun getProviderMode(): MusicProviderMode {
        val raw = prefs.getString(KEY_MODE, MusicProviderMode.BYOK_WEBVIEW.name)
        return runCatching { MusicProviderMode.valueOf(raw!!) }
            .getOrDefault(MusicProviderMode.BYOK_WEBVIEW)
    }

    fun saveVenueType(venueType: CommercialSpaceGuard.VenueType) {
        prefs.edit().putString(KEY_VENUE, venueType.name).apply()
    }

    fun getVenueType(): CommercialSpaceGuard.VenueType {
        val raw = prefs.getString(KEY_VENUE, CommercialSpaceGuard.VenueType.PERSONAL.name)
        return runCatching { CommercialSpaceGuard.VenueType.valueOf(raw!!) }
            .getOrDefault(CommercialSpaceGuard.VenueType.PERSONAL)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_FILE = "narrativedj_b2b"
        private const val KEY_LICENSE = "b2b_license_key"
        private const val KEY_BASE_URL = "b2b_partner_base_url"
        private const val KEY_MODE = "music_provider_mode"
        private const val KEY_VENUE = "venue_type"
    }
}
