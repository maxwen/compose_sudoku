/*
 *  Copyright (C) 2023 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.maxwen.sudoku

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltipBox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.maxwen.sudoku.model.GameDifficulty
import com.maxwen.sudoku.model.MainViewModel
import com.maxwen.sudoku.ui.theme.SudokuTheme
import com.maxwen.sudoku.ui.theme.isTablet
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            SudokuTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val difficulty by viewModel.difficulty.collectAsState()

                    Scaffold(floatingActionButton = {
                        PlainTooltipBox(tooltip = { Text("New Sudoku") }) {
                            FloatingActionButton(
                                modifier = Modifier.tooltipAnchor(),
                                onClick = {
                                    viewModel.resetSelection()
                                    viewModel.createSudoku(difficulty = difficulty)
                                }, containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_grid),
                                    contentDescription = "New",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }) {
                        Column(modifier = Modifier.padding(it)) {
                            Board()
                        }
                    }
                }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
            }
        }
    }

    @Composable
    fun NumberMatrix(
        columns: Int,
        riddleData: List<Int>,
        solveData: List<Int>,
    ) {
        val valueIndex by viewModel.valueIndex.collectAsState()
        val showError by viewModel.showError.collectAsState()

        var size by remember { mutableStateOf(IntSize.Zero) }
        val boxWidth = size.width / columns
        val boxWidthDp: Dp = LocalDensity.current.run { boxWidth.toDp() }
        val boxTextHeightDp: Dp = boxWidthDp * 0.75f
        val fontSize = with(LocalDensity.current) { boxTextHeightDp.toSp() }
        val borderColor = MaterialTheme.colorScheme.onBackground
        val borderColorSecondary = borderColor.copy(alpha = 0.5f)
        val textColor = MaterialTheme.colorScheme.onBackground
        val textColorSolve = MaterialTheme.colorScheme.primary
        val backgroundColor = MaterialTheme.colorScheme.background
        val selectColor = MaterialTheme.colorScheme.secondaryContainer
        val selectColorRowColumn = selectColor.copy(alpha = 0.3f)
        val textColorError = Color.Red

        var valueIndexCoords = Pair(-1, -1)
        if (valueIndex != -1) {
            valueIndexCoords = viewModel.getRowColumnForIndex(valueIndex)
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.onSizeChanged {
                size = it
            }
        ) {
            items(riddleData.size) { index ->
                val riddleValue = riddleData[index]
                val solveValue = solveData[index]
                val coords = viewModel.getRowColumnForIndex(index)
                var boxBackgroundColor = backgroundColor

                if (valueIndex != -1) {
                    val selectedRowColumn =
                        coords.first == valueIndexCoords.first || coords.second == valueIndexCoords.second
                    if (index == valueIndex) {
                        boxBackgroundColor = selectColor
                    } else if (selectedRowColumn) {
                        boxBackgroundColor = selectColorRowColumn
                    }
                }
                val isError =
                    showError && (solveValue > 0 && solveValue != viewModel.getSolveValueAtIndex(
                        index
                    ))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .size(boxWidthDp, boxWidthDp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .size(boxWidthDp, boxWidthDp)
                            .drawWithContent {
                                this@drawWithContent.drawContent()
                                if (coords.first == 3 || coords.first == 6) {
                                    drawLine(
                                        strokeWidth = 2.dp.toPx(),
                                        color = borderColor,
                                        start = Offset(x = 0f, y = 0f),
                                        end = Offset(x = boxWidth.toFloat(), y = 0f),
                                    )
                                }
                                if (coords.second == 3 || coords.second == 6) {
                                    drawLine(
                                        strokeWidth = 2.dp.toPx(),
                                        color = borderColor,
                                        start = Offset(x = 0f, y = 0f),
                                        end = Offset(
                                            x = 0f,
                                            y = boxWidth.toFloat()
                                        ),
                                    )
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(boxWidthDp, boxWidthDp)
                                .background(boxBackgroundColor)
                                .border(
                                    border = BorderStroke(
                                        width = 0.5.dp,
                                        color = borderColorSecondary
                                    )
                                )
                                .clickable(enabled = viewModel.isSudokuCreated() && riddleValue == 0) {
                                    if (riddleValue == 0) {
                                        viewModel.setValueIndex(index)
                                        viewModel.updatePossibleValue()
                                    }
                                }
                                .align(Alignment.CenterHorizontally)

                        ) {
                            if (riddleValue != 0) {
                                Text(
                                    text = riddleValue.toString(),
                                    fontSize = fontSize,
                                    color = textColor,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .wrapContentHeight()
                                )
                            } else if (solveValue != 0) {
                                Text(
                                    text = solveValue.toString(),
                                    fontSize = fontSize,
                                    color = if (isError) textColorError else textColorSolve,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .wrapContentHeight()
                                )
                            } else {
                                Text(
                                    text = "",
                                    fontSize = fontSize,
                                    color = textColor,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .wrapContentHeight()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun NumberLine(columns: Int) {
        val valueIndex by viewModel.valueIndex.collectAsState()
        val possibleValues by viewModel.possibleValues.collectAsState()
        val hideImpossible by viewModel.hideImpossible.collectAsState()

        val selectList = (1..9).map { it }
        var size by remember { mutableStateOf(IntSize.Zero) }
        val boxWidth = size.width / columns
        val boxWidthDp = LocalDensity.current.run { boxWidth.toDp() }
        val boxTextHeightDp: Dp = boxWidthDp * 0.75f
        val fontSize = with(LocalDensity.current) { boxTextHeightDp.toSp() }
        val borderColor = MaterialTheme.colorScheme.onBackground
        val textColor = MaterialTheme.colorScheme.onBackground
        val backgroundColor = MaterialTheme.colorScheme.background

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.onSizeChanged {
                size = it
            }) {
            items(selectList.size) { index ->
                val v = selectList[index]
                val possibleValue = if (hideImpossible) possibleValues.contains(v) else true
                val text = if (possibleValue) v.toString() else ""

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(boxWidthDp, boxWidthDp)
                        .border(
                            border = BorderStroke(
                                width = 1.dp,
                                color = borderColor
                            )
                        )
                        .background(backgroundColor)
                        .clickable(enabled = viewModel.isSudokuCreated() && possibleValue) {
                            if (valueIndex != -1) {
                                viewModel.setValueSelect(index + 1)
                                viewModel.setNumberInRiddle(
                                    valueIndex,
                                    viewModel.valueSelect.value
                                )
                            }
                        }
                ) {
                    if (valueIndex == -1) {
                        Text(
                            text = "",
                            fontSize = fontSize,
                            color = textColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .wrapContentHeight()

                        )
                    } else {
                        Text(
                            text = text,
                            fontSize = fontSize,
                            color = textColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .wrapContentHeight(),
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Buttons() {
        val valueIndex by viewModel.valueIndex.collectAsState()
        val difficulty by viewModel.difficulty.collectAsState()
        val showError by viewModel.showError.collectAsState()
        val hideImpossible by viewModel.hideImpossible.collectAsState()
        val solveList by viewModel.solveList.collectAsState()

        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.weight(1.0f))
                PlainTooltipBox(tooltip = { Text("Clear box") }) {
                    FilledIconButton(
                        onClick = {
                            if (valueIndex != -1) {
                                viewModel.setNumberInRiddle(
                                    valueIndex,
                                    0
                                )
                            }
                        },
                        modifier = Modifier
                            .width(72.dp)
                            .padding(4.dp)
                            .tooltipAnchor(),
                        enabled = valueIndex != -1
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_grid_clear),
                            contentDescription = "Clear"
                        )
                    }
                }
                PlainTooltipBox(tooltip = { Text("Solve box") }) {
                    FilledIconButton(
                        onClick = {
                            if (valueIndex != -1) {
                                viewModel.setNumberInRiddle(
                                    valueIndex,
                                    viewModel.getSolveValueAtIndex(valueIndex)
                                )
                            }
                        },
                        modifier = Modifier
                            .width(72.dp)
                            .padding(4.dp)
                            .tooltipAnchor(),
                        enabled = valueIndex != -1
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_grid_solve),
                            contentDescription = "Solve"
                        )
                    }
                }
                /*PlainTooltipBox(tooltip = { Text("Solve grid") }) {
                    FilledIconButton(
                        onClick = {
                            viewModel.fillAllSolveValues()
                        },
                        modifier = Modifier
                            .width(72.dp)
                            .padding(4.dp)
                            .tooltipAnchor(),
                        enabled = viewModel.isSudokuCreated(),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_grid_solve),
                            contentDescription = "Solve grid"
                        )
                    }
                }*/
                PlainTooltipBox(tooltip = { Text("Clear grid") }) {
                    FilledIconButton(
                        onClick = {
                            viewModel.resetSelection()
                            viewModel.resetRiddle()
                        },
                        modifier = Modifier
                            .width(72.dp)
                            .padding(4.dp)
                            .tooltipAnchor(),
                        enabled = viewModel.isSudokuCreated() && viewModel.isSudokuFilled()
                    ) {
                        Icon(
                            painterResource(id = R.drawable.ic_grid_reset),
                            contentDescription = "Reset"
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1.0f))
            }
            Spacer(modifier = Modifier.height(10.dp))

            val difficultyList =
                listOf(GameDifficulty.EASY, GameDifficulty.MEDIUM, GameDifficulty.EXPERT)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                difficultyList.forEach { value ->
                    Row(
                        modifier = Modifier
                            .height(44.dp)
                            .selectable(
                                selected = (difficulty == value),
                                onClick = { viewModel.setDifficulty(value) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        RadioButton(
                            selected = difficulty == value,
                            onClick = null // null recommended for accessibility with screenreaders
                        )
                        Text(
                            value.label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            )
            {
                Row(
                    Modifier
                        .height(44.dp)
                        .toggleable(
                            value = showError,
                            onValueChange = { viewModel.setShowError(!showError) },
                            role = Role.Checkbox
                        )
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = showError,
                        onCheckedChange = null // null recommended for accessibility with screenreaders
                    )
                    Text(
                        "Show error",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
                Row(
                    Modifier
                        .height(44.dp)
                        .toggleable(
                            value = hideImpossible,
                            onValueChange = { viewModel.setHideImpossible(!hideImpossible) },
                            role = Role.Checkbox
                        )
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = hideImpossible,
                        onCheckedChange = null // null recommended for accessibility with screenreaders
                    )
                    Text(
                        text = "Hide impossible",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun Board() {
        val riddleData by viewModel.riddleList.collectAsState()
        val solveData by viewModel.solveList.collectAsState()
        val isSolved by viewModel.isSolved.collectAsState()

        val columns = 9
        val landscape =
            LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        val borderColor = MaterialTheme.colorScheme.onBackground

        BoxWithConstraints {
            Log.d(
                "sudoku",
                "maxWidth = $maxWidth isTablet = $isTablet landscape = $landscape"
            )
            val mw = maxWidth
            if (mw < 400.dp || (isTablet && !landscape)) {
                Row {
                    if (isTablet) {
                        Spacer(modifier = Modifier.weight(0.1f))
                    }
                    Column(
                        modifier = Modifier
                            .weight(0.4f)
                            .padding(16.dp),
                    ) {
                        Spacer(modifier = Modifier.weight(0.1f))
                        Column(
                            modifier = Modifier.border(
                                border = BorderStroke(
                                    width = 2.dp,
                                    color = if (isSolved) Color.Green else borderColor
                                )
                            )
                        ) {
                            NumberMatrix(
                                columns = columns,
                                riddleData = riddleData,
                                solveData = solveData
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Column(
                            modifier = Modifier.border(
                                border = BorderStroke(
                                    width = 2.dp,
                                    color = borderColor
                                )
                            )
                        ) {
                            NumberLine(columns = columns)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Buttons()
                        Spacer(modifier = Modifier.weight(0.1f))
                    }
                    if (isTablet) {
                        Spacer(modifier = Modifier.weight(0.1f))
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                ) {
                    Spacer(modifier = Modifier.weight(0.1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.weight(0.1f))
                        Column(
                            modifier = Modifier
                                .weight(0.45f)
                                .border(
                                    border = BorderStroke(
                                        width = 2.dp,
                                        color = if (isSolved) Color.Green else borderColor
                                    )
                                )
                        ) {
                            NumberMatrix(
                                columns = columns,
                                riddleData = riddleData,
                                solveData = solveData
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(0.45f)) {
                            Column(
                                modifier = Modifier.border(
                                    border = BorderStroke(
                                        width = 2.dp,
                                        color = borderColor
                                    )
                                )
                            ) {
                                NumberLine(columns = columns)
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Buttons()
                        }
                        Spacer(modifier = Modifier.weight(0.1f))
                    }
                    Spacer(modifier = Modifier.weight(0.1f))
                }
            }
        }
    }
}
