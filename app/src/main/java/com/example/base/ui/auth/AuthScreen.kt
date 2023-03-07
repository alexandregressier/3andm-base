package com.example.base.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.base.state.AuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    vm: AuthViewModel,
) {
    val authState by vm.state.collectAsState()

    var email by remember { mutableStateOf("alex_012@mail.com") }
    var password by remember { mutableStateOf("P@ssw0rd!") }
    val signUpError by vm.signUpError.collectAsState()

    var verificationCode by remember { mutableStateOf("") }
    val verifyError by vm.verifyError.collectAsState()

    var email2 by remember { mutableStateOf("alex_012@mail.com") }
    var password2 by remember { mutableStateOf("P@ssw0rd!") }
    val signInError by vm.signInError.collectAsState()

    Column(
        Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (authState is AuthState.Unauthenticated) "Unauthenticated" else authState.toString(),
            color = when (authState) {
                is AuthState.Unauthenticated -> Color.Red
                is AuthState.Confirming -> Color.Blue
                is AuthState.Authenticated -> Color.Green
            },
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))


        // SIGN UP —————————————————————————————————————————————————————————————————————————————————
        Text(
            "Sign up",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleMedium,
        )
        TextField(
            value = email,
            onValueChange = { email = it; vm.resetSignUpError() },
        )
        TextField(
            value = password,
            onValueChange = { password = it },
        )
        if (signUpError) {
            Text(
                "User already exists.",
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = {
                vm.signUp(email, password)
            }
        ) {
            Text("Sign up")
        }
        Spacer(Modifier.height(8.dp))

        // VERIFY ——————————————————————————————————————————————————————————————————————————————————
        Text(
            "Verification",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleMedium,
        )
        TextField(
            value = verificationCode,
            onValueChange = { verificationCode = it; vm.resetVerifyError() },
        )
        if (verifyError) {
            Text(
                "Wrong verification code.",
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = {
                vm.verify(verificationCode)
            }
        ) {
            Text("Verify")
        }
        Spacer(Modifier.height(8.dp))

        // SIGN IN —————————————————————————————————————————————————————————————————————————————————
        Text(
            "Sign in",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleMedium,
        )
        TextField(
            value = email2,
            onValueChange = { email2 = it; vm.resetSignInError() },
        )
        TextField(
            value = password2,
            onValueChange = { password2 = it; vm.resetSignInError() },
        )
        if (signInError) {
            Text(
                "Wrong email or password.",
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = {
                vm.signIn(email2, password2)
            }
        ) {
            Text("Sign in")
        }
        Spacer(Modifier.height(8.dp))

        // SIGN OUT ————————————————————————————————————————————————————————————————————————————————
        Text(
            "Sign out",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleMedium,
        )
        Button(
            onClick = {
                vm.signOut()
            }
        ) {
            Text("Sign out")
        }
        Spacer(Modifier.height(8.dp))
    }
}