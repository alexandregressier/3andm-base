package com.example.base

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.base.ui.auth.AuthScreen
import com.example.base.ui.auth.AuthViewModel
import com.example.base.ui.theme.BaseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseTheme {
                AuthScreen(
                    vm = AuthViewModel(),
                )
            }
        }
    }
}