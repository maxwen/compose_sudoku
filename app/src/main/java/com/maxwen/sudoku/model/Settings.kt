package com.maxwen.sudoku.model

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.first

object Settings {
    private val TAG = "Settings"

    lateinit var myDataStore: DataStore<Preferences>
    const val SETTINGS_STORE_NAME = "user_preferences"
    private val DIFFICULTY = intPreferencesKey("difficulty")
    private val SOLVE_LIST = stringPreferencesKey("solveList")
    private val MATRIX_LIST = stringPreferencesKey("matrixList")
    private val RIDDLE_LIST = stringPreferencesKey("riddleList")
    private val HIDE_IMPOSSIBLE = booleanPreferencesKey("hideImpossible")
    private val SHOW_ERROR = booleanPreferencesKey("showError")

    private const val SEPARATOR = ","


    suspend fun setDifficulty(value: GameDifficulty) {
        Result.runCatching {
            myDataStore.edit { settings ->
                settings[DIFFICULTY] = value.ordinal
            }
        }
    }

    suspend fun getDifficualty(): GameDifficulty {
        val settings = myDataStore.data.first().toPreferences()
        val intValue = settings[DIFFICULTY] ?: GameDifficulty.MEDIUM.ordinal
        return GameDifficulty.entries[intValue]
    }

    suspend fun setHideImpossible(value: Boolean) {
        Result.runCatching {
            myDataStore.edit { settings ->
                settings[HIDE_IMPOSSIBLE] = value
            }
        }
    }

    suspend fun getHideImpossible(): Boolean {
        val settings = myDataStore.data.first().toPreferences()
        return settings[HIDE_IMPOSSIBLE] ?: false
    }

    suspend fun setShowError(value: Boolean) {
        Result.runCatching {
            myDataStore.edit { settings ->
                settings[SHOW_ERROR] = value
            }
        }
    }

    suspend fun getShowError(): Boolean {
        val settings = myDataStore.data.first().toPreferences()
        return settings[SHOW_ERROR] ?: false
    }


    suspend fun saveRiddle(matrixList: List<Int>, riddleList: List<Int>) {
        Result.runCatching {
            myDataStore.edit { settings ->
                settings[MATRIX_LIST] = matrixList.joinToString(SEPARATOR)
                settings[RIDDLE_LIST] = riddleList.joinToString(SEPARATOR)
            }
        }
    }

    suspend fun clearRiddle() {
        Result.runCatching {
            myDataStore.edit { settings ->
                settings.remove(MATRIX_LIST)
                settings.remove(RIDDLE_LIST)
                settings.remove(SOLVE_LIST)
            }
        }
    }

    suspend fun saveSolveList(solveList: List<Int>) {
        Result.runCatching {
            myDataStore.edit { settings ->
                settings[SOLVE_LIST] = solveList.joinToString(SEPARATOR)
            }
        }
    }

    suspend fun restoreRiddleAndSolveList(
        matrixList: MutableList<Int>,
        riddleList: MutableList<Int>,
        solveList: MutableList<Int>
    ) {
        val settings = myDataStore.data.first().toPreferences()
        settings[MATRIX_LIST]?.let {
            val items = it.split(SEPARATOR)
            items.forEach { item -> matrixList.add(item.trim().toInt()) }
        }
        settings[RIDDLE_LIST]?.let {
            val items = it.split(SEPARATOR)
            items.forEach { item -> riddleList.add(item.trim().toInt()) }
        }
        settings[SOLVE_LIST]?.let {
            val items = it.split(SEPARATOR)
            items.forEach { item -> solveList.add(item.trim().toInt()) }
        }
    }
}
