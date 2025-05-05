package com.example.simplecalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CalculatorApp()
            }
        }
    }
}

@Composable
fun CalculatorApp() {
    var firstNumber by remember { mutableStateOf("") }
    var secondNumber by remember { mutableStateOf("") }
    var operator by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf("") }

    fun clearAll() {
        firstNumber = ""
        secondNumber = ""
        operator = null
        result = ""
    }

    fun appendDigit(digit: String) {
        if (operator == null) {
            firstNumber += digit
        } else {
            secondNumber += digit
        }
    }

    fun calculate() {
        val num1 = firstNumber.toDoubleOrNull() ?: return
        val num2 = secondNumber.toDoubleOrNull() ?: return
        val res = when (operator) {
            "+" -> num1 + num2
            "-" -> num1 - num2
            "*" -> num1 * num2
            "/" -> if (num2 != 0.0) num1 / num2 else Double.NaN
            else -> return
        }
        result = if (res.isNaN()) "Error" else res.toString().removeSuffix(".0")
        firstNumber = result
        secondNumber = ""
        operator = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF202020))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Display
        Text(
            text = if (result.isNotEmpty()) result else (if (operator == null) firstNumber else secondNumber.ifEmpty { "0" }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            fontSize = 48.sp,
            color = Color.White,
            textAlign = TextAlign.End,
            fontWeight = FontWeight.Bold
        )
        // Buttons Grid
        val buttons = listOf(
            listOf("C", "/", "*", "−"),
            listOf("7", "8", "9", "+"),
            listOf("4", "5", "6", "%"),
            listOf("1", "2", "3", "="),
            listOf("0", ".")
        )
        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                row.forEach { label ->
                    Button(
                        onClick = {
                            when (label) {
                                "C" -> clearAll()
                                "=" -> calculate()
                                in listOf("+", "-", "*", "/") -> if (firstNumber.isNotEmpty()) operator = label
                                "." -> appendDigit(label)
                                else -> appendDigit(label)
                            }
                        },
                        modifier = Modifier
                            .weight(
                                when (label) {
                                    "0" -> 2f
                                    else -> 1f
                                }
                            )
                            .aspectRatio(1f),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = if (label in listOf("+", "-", "*", "/", "=", "%")) Color(0xFFFF9800) else Color(0xFF333333))
                    ) {
                        Text(
                            text = label,
                            fontSize = 24.sp,
                            color = Color.White
                        )
                    }
                }
                // Fill remaining space if row has only 2 items
                if (row.size == 2) {
                    Spacer(modifier = Modifier.weight(2f))
                }
            }
        }
    }
}
