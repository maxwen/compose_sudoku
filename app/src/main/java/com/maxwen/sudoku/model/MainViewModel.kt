package com.maxwen.sudoku.model

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.sfuhrm.sudoku.Creator
import de.sfuhrm.sudoku.GameMatrix
import de.sfuhrm.sudoku.GameSchema
import de.sfuhrm.sudoku.GameSchemas
import de.sfuhrm.sudoku.Riddle
import de.sfuhrm.sudoku.Solver
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private var matrix: GameMatrix? = null
    private var riddle: Riddle? = null
    private val matrixList = MutableStateFlow<List<Int>>(mutableListOf())

    val riddleList = MutableStateFlow<List<Int>>(mutableListOf())
    val solveList = MutableStateFlow<List<Int>>(mutableListOf())
    val isSolved = MutableStateFlow<Boolean>(false)
    var valueIndex = MutableStateFlow<Int>(-1)
    var possibleValues = MutableStateFlow<List<Int>>(mutableListOf())
    var valueSelect = MutableStateFlow<Int>(-1)

    fun isSudokuCreated(): Boolean {
        return matrix != null
    }

    fun createSudoku(
        schema: GameSchema = GameSchemas.SCHEMA_9X9,
        difficulty: GameDifficulty = GameDifficulty.MEDIUM
    ) {
        viewModelScope.launch {
            matrix = Creator.createFull(schema)
            riddle = Creator.createRiddle(matrix, difficulty.numbersToClear)
            matrixList.update { getFullMatrixAsList(matrix) }
            riddleList.update { getFullMatrixAsList(riddle) }
            solveList.update { getEmptyMatrixAsList(riddle) }
            isSolved.update { false }
        }
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

    fun setNumberInMatrix(index: Int, value: Int) {
        val matrixList = mutableListOf<Int>()
        matrixList.addAll(solveList.value)
        if (matrixList[index] != -1) {
            matrixList[index] = value

            val pos = getRowColumnForIndex(index)
            riddle?.set(pos.first, pos.second, value.toByte())

            solveList.update {
                matrixList
            }
            updateisSolved()
        }
    }

    fun resetRiddle() {
        if (riddle != null) {
            var index = 0
            for (row in 0..8) {
                for (column in 0..8) {
                    riddle!!.set(row, column, riddleList.value.get(index).toByte())
                    index++
                }
            }
            riddleList.update { getFullMatrixAsList(riddle) }
            solveList.update { getEmptyMatrixAsList(riddle) }
            updateisSolved()
        }
    }

    private fun updateisSolved() {
        if (riddle != null) {
            isSolved.update { solveList.value.none { value -> value == 0 } && riddle!!.isValid }
        }
    }

    fun getSolveValueAtIndex(index: Int): Int {
        return matrixList.value[index]
    }

    fun fillAllSolveValues() {
        solveList.value.forEachIndexed { index, value ->
            if (value == 0) {
                setNumberInMatrix(index, getSolveValueAtIndex(index))
            }
        }
    }

    fun getRowColumnForIndex(index: Int): Pair<Int, Int> {
        val row = index / 9
        val col = if (row == 0) index else index - row.coerceAtLeast(1) * 9
        return Pair(row, col)
    }

    fun getPossibleValuesAtIndex(index: Int): List<Int> {
        val valueList = mutableListOf<Int>()
        val pos = getRowColumnForIndex(index)
        for (value in 1..9) {
            if (riddle!!.canSet(pos.first, pos.second, value.toByte())) {
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
}