package com.friday.assistant

import android.app.Application
import com.friday.assistant.data.local.FridayDatabase
import com.friday.assistant.data.preferences.PreferencesManager

class FridayApp : Application() {

    lateinit var database: FridayDatabase
        private set

    lateinit var preferencesManager: PreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = FridayDatabase.getInstance(this)
        preferencesManager = PreferencesManager(this)
    }
}

