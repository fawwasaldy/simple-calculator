package com.example.simplecalculator

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.DecimalFormat

data class CalculatorState(
    val number1: String = "",
    val number2: String = "",
    val operator: String? = null
) {
    val displayValue: String
        get() = if (number2.isNotEmpty()) number2 else number1.ifEmpty { "0" }
}

sealed class CalculatorAction {
    data class Number(val value: Int) : CalculatorAction()
    data class Operation(val operator: String) : CalculatorAction()
    object Decimal : CalculatorAction()
    object Calculate : CalculatorAction()
    object Clear : CalculatorAction()
    object Delete : CalculatorAction()
}

class CalculatorViewModel : ViewModel() {

    private val _state = MutableStateFlow(CalculatorState())
    val state = _state.asStateFlow()

    fun onAction(action: CalculatorAction) {
        when (action) {
            is CalculatorAction.Number -> enterNumber(action.value)
            is CalculatorAction.Operation -> setOperator(action.operator)
            CalculatorAction.Decimal -> enterDecimal()
            CalculatorAction.Calculate -> performCalculation()
            CalculatorAction.Clear -> clearAll()
            CalculatorAction.Delete -> performDelete()
        }
    }

    private fun enterNumber(number: Int) {
        _state.update { currentState ->
            if (currentState.operator == null) {
                if (currentState.number1.length >= 10) return@update currentState
                val newNumber1 = if (currentState.number1 == "0") number.toString() else currentState.number1 + number
                currentState.copy(number1 = newNumber1)
            } else {
                if (currentState.number2.length >= 10) return@update currentState
                val newNumber2 = if (currentState.number2 == "0") number.toString() else currentState.number2 + number
                currentState.copy(number2 = newNumber2)
            }
        }
    }

    private fun setOperator(operator: String) {
        _state.update { currentState ->
            val num1 = currentState.number1
            val num2 = currentState.number2
            val currentOp = currentState.operator

            if (num1.isNotBlank() && num2.isNotBlank() && currentOp != null) {
                val num1Double = num1.toDoubleOrNull()
                val num2Double = num2.toDoubleOrNull()
                if (num1Double != null && num2Double != null) {
                    val result = doMath(num1Double, num2Double, currentOp)
                    return@update CalculatorState(
                        number1 = formatResult(result),
                        operator = operator,
                        number2 = ""
                    )
                }
            }

            if (num1.isNotBlank()) {
                return@update currentState.copy(operator = operator)
            }

            currentState
        }
    }

    private fun enterDecimal() {
        _state.update { currentState ->
            if (currentState.operator == null) {
                if (!currentState.number1.contains(".")) {
                    currentState.copy(number1 = currentState.number1.ifEmpty { "0" } + ".")
                } else { currentState }
            } else {
                if (!currentState.number2.contains(".")) {
                    currentState.copy(number2 = currentState.number2.ifEmpty { "0" } + ".")
                } else { currentState }
            }
        }
    }

    private fun performCalculation() {
        _state.update { currentState ->
            val number1 = currentState.number1.toDoubleOrNull()
            val number2 = currentState.number2.toDoubleOrNull()

            if (number1 != null && number2 != null && currentState.operator != null) {
                val result = doMath(number1, number2, currentState.operator)
                if (result.isNaN()) {
                    CalculatorState(number1 = "Error")
                } else {
                    CalculatorState(number1 = formatResult(result))
                }
            } else {
                currentState
            }
        }
    }

    private fun doMath(number1: Double, number2: Double, operator: String): Double {
        return when (operator) {
            "+" -> number1 + number2
            "-" -> number1 - number2
            "×" -> number1 * number2
            "÷" -> if (number2 != 0.0) number1 / number2 else Double.NaN
            else -> 0.0
        }
    }


    private fun clearAll() {
        _state.value = CalculatorState()
    }

    private fun performDelete() {
        _state.update {
            when {
                it.number2.isNotBlank() -> it.copy(number2 = it.number2.dropLast(1))
                it.operator != null -> it.copy(operator = null)
                it.number1.isNotBlank() -> it.copy(number1 = it.number1.dropLast(1))
                else -> it
            }
        }
    }

    private fun formatResult(number: Double): String {
        if (number.isInfinite() || number.isNaN()) return "Error"
        val formatter = DecimalFormat("0.########")
        return formatter.format(number)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel: CalculatorViewModel by viewModels()

        setContent {
            HideSystemBars()
            MaterialTheme {
                val state by viewModel.state.collectAsState()
                CalculatorScreen(
                    state = state,
                    onAction = viewModel::onAction
                )
            }
        }
    }
}

@Composable
private fun HideSystemBars() {
    val view = LocalView.current
    if (!view.isInEditMode) {
        LaunchedEffect(Unit) {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

@Composable
fun CalculatorScreen(
    state: CalculatorState,
    onAction: (CalculatorAction) -> Unit
) {
    val buttonSpacing = 12.dp

    val Orange = Color(0xFFF1A33B)
    val LightGray = Color(0xFFA5A5A5)
    val DarkGray = Color(0xFF333333)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
            .systemBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(buttonSpacing),
            horizontalAlignment = Alignment.End
        ) {

            Text(
                text = "Simple Calculator",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 20.sp,
                textAlign = TextAlign.End,
            )

            Text(
                text = state.displayValue,
                color = Color.White,
                fontSize = 80.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(buttonSpacing)) {
                CalculatorButton(symbol = "C", modifier = Modifier.weight(1f), color = LightGray) { onAction(CalculatorAction.Clear) }
                CalculatorButton(symbol = "⌫", modifier = Modifier.weight(1f), color = LightGray) { onAction(CalculatorAction.Delete) }
                CalculatorButton(symbol = "÷", modifier = Modifier.weight(1f), color = Orange) { onAction(CalculatorAction.Operation("÷")) }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(buttonSpacing)) {
                CalculatorButton(symbol = "7", modifier = Modifier.weight(1f), color = DarkGray) { onAction(CalculatorAction.Number(7)) }
                CalculatorButton(symbol = "8", modifier = Modifier.weight(1f), color = DarkGray) { onAction(CalculatorAction.Number(8)) }
                CalculatorButton(symbol = "9", modifier = Modifier.weight(1f), color = DarkGray) { onAction(CalculatorAction.Number(9)) }
                CalculatorButton(symbol = "×", modifier = Modifier.weight(1f), color = Orange) { onAction(CalculatorAction.Operation("×")) }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(buttonSpacing)) {
                CalculatorButton(symbol = "4", modifier = Modifier.weight(1f), color = DarkGray) { onAction(CalculatorAction.Number(4)) }
                CalculatorButton(symbol = "5", modifier = Modifier.weight(1f), color = DarkGray) { onAction(CalculatorAction.Number(5)) }
                CalculatorButton(symbol = "6", modifier = Modifier.weight(1f), color = DarkGray) { onAction(CalculatorAction.Number(6)) }
                CalculatorButton(symbol = "-", modifier = Modifier.weight(1f), color = Orange) { onAction(CalculatorAction.Operation("-")) }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(buttonSpacing)) {
                CalculatorButton(symbol = "1", modifier = Modifier.weight(1f), color = DarkGray) { onAction(CalculatorAction.Number(1)) }
                CalculatorButton(symbol = "2", modifier = Modifier.weight(1f), color = DarkGray) { onAction(CalculatorAction.Number(2)) }
                CalculatorButton(symbol = "3", modifier = Modifier.weight(1f), color = DarkGray) { onAction(CalculatorAction.Number(3)) }
                CalculatorButton(symbol = "+", modifier = Modifier.weight(1f), color = Orange) { onAction(CalculatorAction.Operation("+")) }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(buttonSpacing)) {
                CalculatorButton(symbol = "0", modifier = Modifier.weight(2.15f), color = DarkGray) { onAction(CalculatorAction.Number(0)) }
                CalculatorButton(symbol = ".", modifier = Modifier.weight(1f), color = DarkGray) { onAction(CalculatorAction.Decimal) }
                CalculatorButton(symbol = "=", modifier = Modifier.weight(1f), color = Orange) { onAction(CalculatorAction.Calculate) }
            }
        }
    }
}

@Composable
fun RowScope.CalculatorButton(
    symbol: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .clip(CircleShape)
            .aspectRatio(if (symbol == "0") 2f else 1f),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(text = symbol, fontSize = 36.sp, color = Color.White)
    }
}

@Preview(showBackground = true, device = "spec:shape=Normal,width=360,height=740,unit=dp,dpi=480")
@Composable
fun DefaultPreview() {
    MaterialTheme {
        CalculatorScreen(state = CalculatorState(number1 = "123.45"), onAction = {})
    }
}
