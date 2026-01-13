package com.tendertracker.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tendertracker.domain.model.User
import com.tendertracker.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val currentUser: User? = null,
    val error: String? = null,
    // Login form
    val loginEmail: String = "",
    val loginPassword: String = "",
    // Register form
    val registerEmail: String = "",
    val registerPassword: String = "",
    val registerConfirmPassword: String = "",
    val registerFullName: String = "",
    val registerCompanyName: String = ""
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.isLoggedIn.collect { isLoggedIn ->
                _state.update { it.copy(isLoggedIn = isLoggedIn) }
            }
        }
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _state.update { it.copy(currentUser = user) }
            }
        }
    }

    // Login form handlers
    fun onLoginEmailChange(email: String) {
        _state.update { it.copy(loginEmail = email, error = null) }
    }

    fun onLoginPasswordChange(password: String) {
        _state.update { it.copy(loginPassword = password, error = null) }
    }

    fun onLogin() {
        val email = _state.value.loginEmail.trim()
        val password = _state.value.loginPassword

        if (email.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = "Please enter email and password") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            authRepository.login(email, password)
                .onSuccess { user ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            currentUser = user,
                            isLoggedIn = true,
                            loginEmail = "",
                            loginPassword = ""
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Login failed"
                        )
                    }
                }
        }
    }

    // Register form handlers
    fun onRegisterEmailChange(email: String) {
        _state.update { it.copy(registerEmail = email, error = null) }
    }

    fun onRegisterPasswordChange(password: String) {
        _state.update { it.copy(registerPassword = password, error = null) }
    }

    fun onRegisterConfirmPasswordChange(password: String) {
        _state.update { it.copy(registerConfirmPassword = password, error = null) }
    }

    fun onRegisterFullNameChange(name: String) {
        _state.update { it.copy(registerFullName = name) }
    }

    fun onRegisterCompanyNameChange(name: String) {
        _state.update { it.copy(registerCompanyName = name) }
    }

    fun onRegister() {
        val email = _state.value.registerEmail.trim()
        val password = _state.value.registerPassword
        val confirmPassword = _state.value.registerConfirmPassword
        val fullName = _state.value.registerFullName.trim().ifBlank { null }
        val companyName = _state.value.registerCompanyName.trim().ifBlank { null }

        // Validation
        if (email.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = "Please enter email and password") }
            return
        }
        if (password != confirmPassword) {
            _state.update { it.copy(error = "Passwords do not match") }
            return
        }
        if (password.length < 8) {
            _state.update { it.copy(error = "Password must be at least 8 characters") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            authRepository.register(email, password, fullName, companyName)
                .onSuccess { user ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            currentUser = user,
                            isLoggedIn = true,
                            registerEmail = "",
                            registerPassword = "",
                            registerConfirmPassword = "",
                            registerFullName = "",
                            registerCompanyName = ""
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Registration failed"
                        )
                    }
                }
        }
    }

    fun onLogout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
