package com.example.base

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.kotlin.core.Amplify
import com.example.base.ui.theme.BaseTheme
import kotlinx.coroutines.launch

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

class AuthViewModel() : ViewModel() {

    fun signIn(
        email: String,
        password: String
    ) {
        viewModelScope.launch {
            try {
                val result = Amplify.Auth.signUp(
                    email,
                    password,
                    AuthSignUpOptions.builder()
                        .userAttribute(AuthUserAttributeKey.email(), email)
                        .build()
                )
                Log.i("AuthQuickStart", "Result: $result")
            } catch (error: AuthException) {
                Log.e("AuthQuickStart", "Sign up failed", error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    vm: AuthViewModel,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column {
        TextField(
            value = email,
            onValueChange = { email = it },
        )
        TextField(
            value = password,
            onValueChange = { password = it },
        )
        Button(
            onClick = {
                vm.signIn(email, password)
            }
        ) {
            Text("Sign up")
        }
    }
}