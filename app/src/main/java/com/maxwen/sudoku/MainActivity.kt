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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.maxwen.sudoku.model.GameDifficulty
import com.maxwen.sudoku.model.GameSchemaEnum
import com.maxwen.sudoku.model.MainViewModel
import com.maxwen.sudoku.ui.theme.SudokuTheme
import de.sfuhrm.sudoku.GameSchema
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity(
) : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SudokuTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    Board()
                }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (!viewModel.isSudokuCreated()) {
                    viewModel.createSudoku();
                }
            }
        }
    }

    @Composable
    fun getScreenWidth(): Dp {
        val configuration = LocalConfiguration.current
        return configuration.screenWidthDp.dp
    }

    @Composable
    fun BoardMatrix(
        columns: Int,
        riddleData: List<Int>,
        solveData: List<Int>,
    ) {
        val valueIndex by viewModel.valueIndex.collectAsState()
        var size by remember { mutableStateOf(IntSize.Zero) }
        val boxWidth = size.width / columns
        val boxWidthDp = LocalDensity.current.run { boxWidth.toDp() - 8.dp }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.onSizeChanged {
                size = it
            }
        ) {
            items(riddleData.size) { index ->
                val coords = viewModel.getRowColumnForIndex(index)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(4.dp)
                        .heightIn(min = boxWidthDp)
                        .border(
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (valueIndex == index) Color.Green else Color.DarkGray
                            )
                        )
                        .background(Color.LightGray)

                ) {
                    val value = riddleData[index]
                    val solveValue = solveData[index]

                    if (value != 0) {
                        Text(
                            text = value.toString(),
                            fontSize = 24.sp,
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (solveValue != 0) {
                        Text(
                            text = solveValue.toString(),
                            fontSize = 24.sp,
                            color = Color.Blue,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(enabled = true) {
                                    viewModel.setValueIndex(index)
                                    viewModel.updatePossibleValue()
                                },
                        )
                    } else {
                        Text(
                            text = "",
                            fontSize = 24.sp,
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(enabled = true) {
                                    viewModel.setValueIndex(index)
                                    viewModel.updatePossibleValue()
                                },
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun NumberLine(columns: Int) {
        val valueIndex by viewModel.valueIndex.collectAsState()
        val possibleValues by viewModel.possibleValues.collectAsState()
        val selectList = (1..9).map { it }
        var size by remember { mutableStateOf(IntSize.Zero) }
        val boxWidth = size.width / columns
        val boxWidthDp = LocalDensity.current.run { boxWidth.toDp() - 8.dp }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.onSizeChanged {
                size = it
            }) {
            items(selectList.size) { index ->
                val v = selectList[index]
                val text = v.toString() //if (possibleValues.contains(v)) v.toString() else ""

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(4.dp)
                        .heightIn(min = boxWidthDp)
                        .border(
                            border = BorderStroke(
                                width = 1.dp,
                                color = Color.DarkGray
                            )
                        )
                        .background(Color.LightGray),
                ) {
                    if (valueIndex == -1) {
                        Text(
                            text = "",
                            fontSize = 24.sp,
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        Text(
                            text = text,
                            fontSize = 24.sp,
                            color = if (possibleValues.contains(v)) Color.Black else Color.Red,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(enabled = true) {
                                    viewModel.setValueSelect(index + 1)
                                    viewModel.setNumberInMatrix(
                                        valueIndex,
                                        viewModel.valueSelect.value
                                    )
                                },
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun Buttons() {
        val isSolved by viewModel.isSolved.collectAsState()
        val valueIndex by viewModel.valueIndex.collectAsState()

        Column {
            Row(
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp)
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            ) {
                Button(
                    onClick = {
                        if (valueIndex != -1) {
                            viewModel.setNumberInMatrix(
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
                            viewModel.setNumberInMatrix(
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
                Button(
                    onClick = {
                        viewModel.fillAllSolveValues()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                ) {
                    Text(
                        text = "Solve all"
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp)
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            ) {
                Button(
                    onClick = {
                        viewModel.resetSelection()
                        viewModel.createSudoku()
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
                        .padding(4.dp)
                ) {
                    Text(
                        text = "Reset"
                    )
                }
            }
            if (isSolved) {
                Row(
                    modifier = Modifier
                        .padding(top = 10.dp, start = 8.dp, end = 8.dp)
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                            .background(Color.Green),
                    ) {
                        Text(
                            text = "Solved",
                            fontSize = 24.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Board() {
        val riddleData by viewModel.riddleList.collectAsState()
        val solveData by viewModel.solveList.collectAsState()
        val orientation = LocalConfiguration.current.orientation

        val columns = 9

        BoxWithConstraints {
            Log.d("sudoku", "maxWidth = " + maxWidth)
            if (maxWidth < 400.dp) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    BoardMatrix(
                        columns = columns,
                        riddleData = riddleData,
                        solveData = solveData
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    NumberLine(columns = columns)
                    Spacer(modifier = Modifier.height(10.dp))
                    Buttons()
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Column(modifier = Modifier.weight(0.5f)) {
                        BoardMatrix(
                            columns = columns,
                            riddleData = riddleData,
                            solveData = solveData
                        )
                    }
                    Column(modifier = Modifier.weight(0.5f)) {
                        NumberLine(columns = columns)
                        Spacer(modifier = Modifier.height(10.dp))
                        Buttons()
                    }
                }
            }
        }
    }
}
