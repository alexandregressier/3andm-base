package com.example.base.ui.state

sealed interface AuthState {
    object Unauthenticated : AuthState
    data class Confirming(val emailToConfirm: String, val password: String) : AuthState
    data class Authenticated(val email: String) : AuthState
}