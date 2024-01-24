package com.maxwen.sudoku.model

import de.sfuhrm.sudoku.GameSchema
import de.sfuhrm.sudoku.GameSchemas


enum class GameSchemaEnum(
    private val schema: GameSchema
) {

    /** The 9x9 schema.  */
    S9X9(GameSchemas.SCHEMA_9X9),

}