package com.cohors.app.presentation.common

/**
 * Type-safe, generic UI state wrapper used across all Compose screens
 * (MVI / Unidirectional Data Flow). Every screen renders one of these
 * four states for each data section it displays.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
}
