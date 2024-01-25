package com.maxwen.sudoku.model

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.sfuhrm.sudoku.Creator
import de.sfuhrm.sudoku.GameMatrix
import de.sfuhrm.sudoku.GameMatrixFactory
import de.sfuhrm.sudoku.GameSchema
import de.sfuhrm.sudoku.GameSchemas
import de.sfuhrm.sudoku.Riddle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    // both of these are never changed
    private var matrix: GameMatrix? = null
    private var riddle: Riddle? = null

    // this is the one that changes - starts with contents of riddle
    private var solveMatrix: Riddle? = null
    private val matrixList = MutableStateFlow<List<Int>>(MutableList(81) { 0 })

    val riddleList = MutableStateFlow<List<Int>>(MutableList(81) { 0 })
    val solveList = MutableStateFlow<List<Int>>(MutableList(81) { 0 })
    val isSolved = MutableStateFlow<Boolean>(false)
    var valueIndex = MutableStateFlow<Int>(-1)
    var possibleValues = MutableStateFlow<List<Int>>(mutableListOf())
    var valueSelect = MutableStateFlow<Int>(-1)
    val difficulty = MutableStateFlow<GameDifficulty>(GameDifficulty.MEDIUM)
    var gameSchema: GameSchema = GameSchemas.SCHEMA_9X9

    init {
        viewModelScope.launch {
            difficulty.update { Settings.getDifficualty() }
            try {
                restoreSavedGame()
            } catch (e: Exception) {
                Log.e("sudoku", "restoreSavedGame failed", e)
            }
        }
    }

    private suspend fun restoreSavedGame() {
        val savedMatrixList = mutableListOf<Int>()
        val savedRiddleList = mutableListOf<Int>()
        val savedSolvedList = mutableListOf<Int>()

        Settings.restoreRiddleAndSolveList(savedMatrixList, savedRiddleList, savedSolvedList)
        if (savedMatrixList.isNotEmpty() && savedRiddleList.isNotEmpty()) {
            matrix = GameMatrixFactory().newGameMatrix(gameSchema)
            riddle = GameMatrixFactory().newRiddle(gameSchema)
            solveMatrix = GameMatrixFactory().newRiddle(gameSchema)

            savedMatrixList.forEachIndexed { index, value ->
                setNumberInMatrixInternal(matrix, index, value)
            }
            Log.d("sudoko", "matrix " + matrix)

            savedRiddleList.forEachIndexed { index, value ->
                setNumberInMatrixInternal(riddle, index, value)
            }
            Log.d("sudoko", "riddle " + riddle)

            solveMatrix!!.setAll(riddle!!.array)
            Log.d("sudoko", "solveMatrix " + solveMatrix)

            matrixList.update { getFullMatrixAsList(matrix) }
            riddleList.update { getFullMatrixAsList(riddle) }

            applySolveList(savedSolvedList)
            Log.d("sudoko", "solveMatrix " + solveMatrix)
        }
    }


    fun isSudokuCreated(): Boolean {
        return matrix != null
    }

    fun createSudoku(
        schema: GameSchema = GameSchemas.SCHEMA_9X9,
        difficulty: GameDifficulty = GameDifficulty.MEDIUM
    ) {
        gameSchema = schema
        matrix = Creator.createFull(schema)
        riddle = Creator.createRiddle(matrix, difficulty.numbersToClear)
        solveMatrix = GameMatrixFactory().newRiddle(gameSchema)
        solveMatrix!!.setAll(riddle!!.array)
        matrixList.update { getFullMatrixAsList(matrix) }
        riddleList.update { getFullMatrixAsList(riddle) }
        solveList.update { getEmptyMatrixAsList(riddle) }
        isSolved.update { false }
        saveRiddle()
    }

    private fun getFullMatrixAsList(matrix: GameMatrix?): List<Int> {
        val matrixList = mutableListOf<Int>()
        if (matrix != null) {
            for (row in 0..8) {
                for (column in 0..8) {
                    val value = matrix.get(row, column).toInt()
                    matrixList.add(value)
                }
            }
        }
        return matrixList
    }

    private fun getEmptyMatrixAsList(matrix: GameMatrix?): List<Int> {
        val matrixList = mutableListOf<Int>()
        if (matrix != null) {
            for (row in 0..8) {
                for (column in 0..8) {
                    val value = matrix.get(row, column).toInt()
                    if (value == 0) {
                        matrixList.add(value)
                    } else {
                        matrixList.add(-1)
                    }
                }
            }
        }
        return matrixList
    }

    fun setNumberInRiddle(index: Int, value: Int) {
        val matrixList = mutableListOf<Int>()
        matrixList.addAll(solveList.value)
        if (matrixList[index] != -1) {
            matrixList[index] = value

            val pos = getRowColumnForIndex(index)
            solveMatrix?.set(pos.first, pos.second, value.toByte())

            solveList.update {
                matrixList
            }
            updateisSolved()
            saveState()
        }
    }

    private fun setNumberInSolveMatrix(index: Int, value: Int) {
        val pos = getRowColumnForIndex(index)
        solveMatrix?.set(pos.first, pos.second, value.toByte())
    }

    private fun setNumberInMatrixInternal(matrix: GameMatrix?, index: Int, value: Int) {
        val pos = getRowColumnForIndex(index)
        matrix?.set(pos.first, pos.second, value.toByte())
    }

    fun resetRiddle() {
        if (solveMatrix != null) {
            var index = 0
            for (row in 0..8) {
                for (column in 0..8) {
                    solveMatrix!!.set(row, column, riddle!!.get(row, column))
                    index++
                }
            }
            solveList.update { getEmptyMatrixAsList(riddle) }
            updateisSolved()
            saveState()
        }
    }

    private fun applySolveList(customSolveList: List<Int>) {
        customSolveList.forEachIndexed { index, value ->
            if (value != -1) {
                setNumberInSolveMatrix(index, value)
            }
        }
        solveList.update {
            customSolveList
        }
        updateisSolved()
    }

    private fun updateisSolved() {
        if (riddle != null) {
            isSolved.update { solveList.value.none { value -> value == 0 } && solveMatrix!!.isValid }
        }
    }

    private fun saveRiddle() {
        if (riddle != null) {
            viewModelScope.launch {
                Settings.clearRiddle()
                Settings.saveRiddle(matrixList.value, riddleList.value)
            }
        }
    }

    private fun saveState() {
        viewModelScope.launch {
            Settings.saveSolveList(solveList.value)
        }
    }


    fun getSolveValueAtIndex(index: Int): Int {
        return matrixList.value[index]
    }

    fun fillAllSolveValues() {
        val allSolveList = mutableListOf<Int>()
        allSolveList.addAll(solveList.value)
        solveList.value.forEachIndexed { index, value ->
            if (value == 0) {
                val solveValue = getSolveValueAtIndex(index)
                setNumberInSolveMatrix(index, solveValue)
                allSolveList[index] = solveValue
            }
        }
        solveList.update {
            allSolveList
        }
        updateisSolved()
        saveState()
    }

    fun getRowColumnForIndex(index: Int): Pair<Int, Int> {
        val row = index / 9
        val col = if (row == 0) index else index - row.coerceAtLeast(1) * 9
        return Pair(row, col)
    }

    private fun getPossibleValuesAtIndex(index: Int): List<Int> {
        val valueList = mutableListOf<Int>()
        val pos = getRowColumnForIndex(index)
        for (value in 1..9) {
            if (solveMatrix!!.canSet(pos.first, pos.second, value.toByte())) {
                valueList.add(value)
            }
        }
        return valueList
    }

    fun setValueIndex(index: Int) {
        valueIndex.update { index }
    }


    fun setValueSelect(index: Int) {
        valueSelect.update { index }
    }

    fun updatePossibleValue() {
        possibleValues.update { getPossibleValuesAtIndex(valueIndex.value) }
    }

    fun resetSelection() {
        valueSelect.update { -1 }
        valueIndex.update { -1 }
        possibleValues.update { listOf() }
    }

    fun setDifficulty(value: GameDifficulty) {
        difficulty.update { value }
        viewModelScope.launch {
            Settings.setDifficulty(value)
        }
    }

    fun getDifficulty(): GameDifficulty {
        return difficulty.value
    }
}