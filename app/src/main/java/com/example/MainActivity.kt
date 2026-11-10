package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.TrttMainScreen
import com.example.ui.TrttViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: TrttViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            MyApplicationTheme(darkTheme = isDark, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (isDark) Color(0xFF000000) else Color(0xFFF1F5F9)
                ) {
                    TrttMainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
