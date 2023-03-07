package com.example.base

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CodeMismatchException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UserNotFoundException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UsernameExistsException
import com.amplifyframework.auth.AuthChannelEventName
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.step.AuthSignUpStep
import com.amplifyframework.core.InitializationStatus
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.kotlin.core.Amplify
import com.example.base.ui.theme.BaseTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

sealed interface AuthState {
    object Unauthenticated : AuthState
    data class Confirming(val emailToConfirm: String, val password: String) : AuthState
    data class Authenticated(val email: String) : AuthState
}

class AuthViewModel() : ViewModel() {
    
    companion object {
        private const val TAG = "AuthViewModel"
    }

    private val _state = MutableStateFlow(AuthState.Unauthenticated as AuthState)
    val state = _state.asStateFlow()

    private val _signUpError = MutableStateFlow(false)
    val signUpError = _signUpError.asStateFlow()

    private val _verifyError = MutableStateFlow(false)
    val verifyError = _verifyError.asStateFlow()

    private val _signInError = MutableStateFlow(false)
    val signInError = _signInError.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val session = Amplify.Auth.fetchAuthSession()
                if (session.isSignedIn) {
                    _state.value = AuthState.Authenticated(email = fetchEmail())
                }
            } catch (error: AuthException) {
                Log.e("AmplifyQuickstart", "Failed to fetch auth session", error)
            }
            Amplify.Hub.subscribe(HubChannel.AUTH).collect {
                when (it.name) {
                    InitializationStatus.SUCCEEDED.toString() ->
                        Log.i(TAG, "Auth successfully initialized")

                    InitializationStatus.FAILED.toString() ->
                        Log.i(TAG, "Auth failed to initialize")

                    else -> when (AuthChannelEventName.valueOf(it.name)) {
                        AuthChannelEventName.SIGNED_IN -> {
                            _state.value = AuthState.Authenticated(email = fetchEmail())
                            Log.i(TAG, "Auth just became signed in.")
                        }
                        AuthChannelEventName.SIGNED_OUT -> {
                            _state.value = AuthState.Unauthenticated
                            Log.i(TAG, "Auth just became signed out.")
                        }
                        AuthChannelEventName.SESSION_EXPIRED -> {
                            _state.value = AuthState.Unauthenticated
                            Log.i(TAG, "Auth session just expired.")
                        }
                        AuthChannelEventName.USER_DELETED -> {
                            _state.value = AuthState.Unauthenticated
                            Log.i(TAG, "User has been deleted.")
                        }
                    }
                }
            }
        }
    }

    private suspend fun fetchEmail(): String {
        val attributes = Amplify.Auth.fetchUserAttributes()
        return attributes.first { it.key == AuthUserAttributeKey.email() }.value
    }

    fun signUp(
        email: String,
        password: String
    ) {
        if (_state.value is AuthState.Unauthenticated) {
            viewModelScope.launch {
                try {
                    val result = Amplify.Auth.signUp(
                        email,
                        password,
                        AuthSignUpOptions.builder()
                            .userAttribute(AuthUserAttributeKey.email(), email)
                            .build()
                    )
                    _state.value = when (result.nextStep.signUpStep) {
                        AuthSignUpStep.CONFIRM_SIGN_UP_STEP -> AuthState.Confirming(email, password)
                        AuthSignUpStep.DONE -> AuthState.Authenticated(email)
                    }
                    Log.i(TAG, "Sign up succeeded")
                } catch (error: AuthException) {
                    _signUpError.value = true
                    Log.e(TAG, "Sign up failed", error)
                }
            }
        }
    }

    fun verify(verificationCode: String) {
        if (_state.value is AuthState.Confirming) {
            val (emailToConfirm, password) = state.value as AuthState.Confirming
            viewModelScope.launch {
                try {
                    val result = Amplify.Auth.confirmSignUp(emailToConfirm, verificationCode)
                    if (result.isSignUpComplete) {
                        Log.i(TAG, "Confirm sign up succeeded")
                        Amplify.Auth.signIn(emailToConfirm, password)
                    }
                } catch (error: AuthException) {
                    _verifyError.value = true
                    Log.e(TAG, "Confirm sign up failed", error)
                }
            }
        }
    }

    fun signIn(
        email: String,
        password: String,
    ) {
        if (_state.value !is AuthState.Authenticated) {
            viewModelScope.launch {
                try {
                    val result = Amplify.Auth.signIn(email, password)
                } catch (error: AuthException) {
                    _signInError.value = true
                    Log.e(TAG, "Sign in failed", error)
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                Amplify.Auth.signOut()
            } catch (error: AuthException) {
                Log.e(TAG, "Sign out failed", error)
            }
        }
    }

    fun resetSignUpError() {
        if (_signUpError.value)
            _signUpError.value = false
    }

    fun resetVerifyError() {
        if (_verifyError.value)
            _verifyError.value = false
    }

    fun resetSignInError() {
        if (_signInError.value)
            _signInError.value = false
    }
}

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