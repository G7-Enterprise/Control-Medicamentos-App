package com.carlos.controlmedicamentos.license

sealed interface LicenseStatus {
    data object Loading : LicenseStatus
    data class Valid(
        val type: LicenseType,
        val startDate: Long,
        val endDate: Long
    ) : LicenseStatus

    data class Expired(
        val type: LicenseType,
        val endDate: Long
    ) : LicenseStatus

    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : LicenseStatus
}

enum class LicenseType {
    TRIAL,
    ANNUAL
}
