package com.example.base.ui.state

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amplifyframework.auth.AuthChannelEventName
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.step.AuthSignUpStep
import com.amplifyframework.core.InitializationStatus
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.kotlin.core.Amplify
import com.example.base.data.remote.HttpClient
import com.example.base.data.remote.model.Message
import com.example.base.data.remote.model.MessageInput
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel() : ViewModel() {

    companion object {
        private const val TAG = "AuthViewModel"
        private val BASE_URL = "https://mruiyyiiy0.execute-api.eu-west-1.amazonaws.com"
    }

    private val _state = MutableStateFlow(AuthState.Unauthenticated as AuthState)
    val state = _state.asStateFlow()

    private val _signUpError = MutableStateFlow(false)
    val signUpError = _signUpError.asStateFlow()

    private val _verifyError = MutableStateFlow(false)
    val verifyError = _verifyError.asStateFlow()

    private val _signInError = MutableStateFlow(false)
    val signInError = _signInError.asStateFlow()

    private val _messages = MutableStateFlow(emptyList<Message>())
    val messages = _messages.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val session = Amplify.Auth.fetchAuthSession()
                if (session.isSignedIn) {
                    _state.value = AuthState.Authenticated(email = fetchEmail())
                }
            } catch (error: AuthException) {
                _state.value = AuthState.Unauthenticated
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
        viewModelScope.launch {
            while (true) {
                fetchMessages()
                delay(2_000)
            }
        }
    }

    fun sendMessage(messageContent: String) {
        viewModelScope.launch {
            val session = Amplify.Auth.fetchAuthSession() as? AWSCognitoAuthSession
            val tokens = session?.userPoolTokensResult?.value
            val accessToken = tokens?.accessToken
            if (accessToken == null) {
                Log.e(TAG, "User is not logged in")
                awaitCancellation()
            }
            val response = HttpClient.post("$BASE_URL/messages") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $accessToken")
                }
                contentType(ContentType.Application.Json)
                setBody(MessageInput(content = messageContent))
            }
            when (response.status) {
                HttpStatusCode.OK -> {
                    val message: Message = response.body()
                    Log.i(TAG, "Success: $message")
                }
                HttpStatusCode.Unauthorized -> {
                    Log.e(TAG, "Unauthorized: user must be signed in to use this endpoint")
                }
                else -> {
                    Log.e(TAG, "Unexpected error: ${response.bodyAsText()}")
                }
            }
        }
    }

    suspend fun fetchMessages() {
        val response = HttpClient.get("$BASE_URL/messages") {
            contentType(ContentType.Application.Json)
        }
        when (response.status) {
            HttpStatusCode.OK -> {
                val messages: List<Message> = response.body()
                _messages.value = messages
                Log.i(TAG, "Messages fetched successfully: ${messages.map {it.content}}")
            }
            else -> {
                Log.e(TAG, "Unexpected error: ${response.bodyAsText()}")
            }
        }
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

    private suspend fun fetchEmail(): String {
        val attributes = Amplify.Auth.fetchUserAttributes()
        return attributes.first { it.key == AuthUserAttributeKey.email() }.value
    }
}