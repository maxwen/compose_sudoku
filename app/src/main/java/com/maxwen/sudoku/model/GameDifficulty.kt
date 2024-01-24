package com.maxwen.sudoku.model

enum class GameDifficulty(val numbersToClear: Int) {
    VERY_EASY(30),
    EASY(40),
    MEDIUM(48),
    HARD(52),
    EXPERT(59)
}