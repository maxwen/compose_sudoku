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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            SudokuTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Scaffold {
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
                val text = if (hideImpossible && !possibleValues.contains(v)) "" else v.toString()

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
                        .clickable(enabled = viewModel.isSudokuCreated()) {
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

    @Composable
    fun Buttons() {
        val valueIndex by viewModel.valueIndex.collectAsState()
        val difficulty by viewModel.difficulty.collectAsState()
        val showError by viewModel.showError.collectAsState()
        val hideImpossible by viewModel.hideImpossible.collectAsState()

        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        if (valueIndex != -1) {
                            viewModel.setNumberInRiddle(
                                valueIndex,
                                0
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp),
                    enabled = valueIndex != -1
                ) {
                    Text(
                        text = "Clear"
                    )
                }
                Button(
                    onClick = {
                        if (valueIndex != -1) {
                            viewModel.setNumberInRiddle(
                                valueIndex,
                                viewModel.getSolveValueAtIndex(valueIndex)
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp),
                    enabled = valueIndex != -1
                ) {
                    Text(
                        text = "Solve"
                    )
                }
                /*Button(
                    onClick = {
                        viewModel.fillAllSolveValues()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp),
                    enabled = viewModel.isSudokuCreated(),
                ) {
                    Text(
                        text = "All"
                    )
                }*/
            }
            Spacer(modifier = Modifier.height(5.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        viewModel.resetSelection()
                        viewModel.createSudoku(difficulty = difficulty)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                ) {
                    Text(
                        text = "New"
                    )
                }
                Button(
                    onClick = {
                        viewModel.resetSelection()
                        viewModel.resetRiddle()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp),
                    enabled = viewModel.isSudokuCreated()
                ) {
                    Text(
                        text = "Reset"
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                RadioButton(
                    selected = difficulty == GameDifficulty.EASY,
                    onClick = { viewModel.setDifficulty(GameDifficulty.EASY) }
                )
                Text("Easy")

                RadioButton(
                    selected = difficulty == GameDifficulty.MEDIUM,
                    onClick = { viewModel.setDifficulty(GameDifficulty.MEDIUM) }
                )
                Text("Medium")

                RadioButton(
                    selected = difficulty == GameDifficulty.EXPERT,
                    onClick = { viewModel.setDifficulty(GameDifficulty.EXPERT) }
                )
                Text("Hard")

            }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Checkbox(
                    checked = showError,
                    onCheckedChange = { viewModel.setShowError(!showError) }
                )
                Text("Show error")

                Spacer(modifier = Modifier.width(5.dp))

                Checkbox(
                    checked = hideImpossible,
                    onCheckedChange = { viewModel.setHideImpossible(!hideImpossible) }
                )
                Text("Hide impossible")
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
                        Spacer(modifier = Modifier.height(10.dp))
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
                            Spacer(modifier = Modifier.height(5.dp))
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
