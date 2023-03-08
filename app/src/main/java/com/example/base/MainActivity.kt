package com.example.base

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.base.ui.screens.AppScreen
import com.example.base.ui.state.AppViewModel
import com.example.base.ui.theme.BaseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseTheme {
                AppScreen(
                    vm = AppViewModel(),
                )
            }
        }
    }
}