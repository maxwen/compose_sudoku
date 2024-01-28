package com.maxwen.sudoku.model

enum class GameDifficulty(val numbersToClear: Int, val label: String) {
    VERY_EASY(30, "Very easy"),
    EASY(40, "Easy"),
    MEDIUM(48, "Medium"),
    HARD(52, "Hard"),
    EXPERT(59, "Expert")
}