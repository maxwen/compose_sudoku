package com.maxwen.sudoku

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.maxwen.sudoku.model.Settings

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = Settings.SETTINGS_STORE_NAME
)

class App : Application() {

    override fun onCreate() {
        super.onCreate( )

        Settings.myDataStore = applicationContext.dataStore
    }
}
