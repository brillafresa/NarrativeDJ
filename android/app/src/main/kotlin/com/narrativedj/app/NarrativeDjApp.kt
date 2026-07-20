package com.narrativedj.app

import android.app.Application
import com.narrativedj.app.locale.AppLocaleStore

/** Applies stored locale before any Activity starts. */
class NarrativeDjApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLocaleStore.applyStoredLocale(this)
    }
}
