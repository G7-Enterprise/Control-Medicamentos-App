package com.carlos.controlmedicamentos.license

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ActivationUiState {
    data object Idle : ActivationUiState
    data object Validating : ActivationUiState
    data object Success : ActivationUiState
    data class Error(val message: String) : ActivationUiState
}

class LicenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LicenseRepository = FirebaseLicenseRepository(application)

    private val _status = MutableStateFlow<LicenseStatus>(LicenseStatus.Loading)
    val status: StateFlow<LicenseStatus> = _status
    val syncDebug: StateFlow<LicenseSyncDebug> = repository.syncDebug

    private val _activationState = MutableStateFlow<ActivationUiState>(ActivationUiState.Idle)
    val activationState: StateFlow<ActivationUiState> = _activationState

    init {
        verifyLicense()
    }

    fun verifyLicense() {
        _status.value = LicenseStatus.Loading
        viewModelScope.launch {
            _status.value = runCatching { repository.verifyLicense() }
                .getOrElse { error ->
                    LicenseStatus.Error(
                        message = error.message ?: "No se pudo verificar la licencia.",
                        canRetry = true
                    )
                }
        }
    }

    fun activate(licenseKey: String) {
        if (_activationState.value == ActivationUiState.Validating) return
        _activationState.value = ActivationUiState.Validating
        viewModelScope.launch {
            when (repository.activateWithKey(licenseKey)) {
                is ActivationResult.Success -> {
                    _activationState.value = ActivationUiState.Success
                    verifyLicense()
                }
                is ActivationResult.InvalidKey ->
                    _activationState.value = ActivationUiState.Error(
                        "La llave ingresada no es válida. Revísala e inténtalo de nuevo."
                    )
                is ActivationResult.ExpiredOrDisabledKey ->
                    _activationState.value = ActivationUiState.Error(
                        "Esta llave está expirada o deshabilitada. Contacta a soporte."
                    )
                is ActivationResult.NetworkError ->
                    _activationState.value = ActivationUiState.Error(
                        "No se pudo conectar con el servidor. Revisa tu conexión a internet."
                    )
                is ActivationResult.FirestoreError ->
                    _activationState.value = ActivationUiState.Error(
                        "La llave es válida, pero no se pudo registrar la licencia. Inténtalo de nuevo."
                    )
            }
        }
    }

    fun resetActivationState() {
        _activationState.value = ActivationUiState.Idle
    }
}
