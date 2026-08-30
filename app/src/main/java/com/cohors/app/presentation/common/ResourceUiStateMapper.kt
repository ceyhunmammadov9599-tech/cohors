package com.cohors.app.presentation.common

import com.cohors.app.core.util.Resource

/**
 * Maps a repository-level [Resource] (Loading/Success/Error) into the
 * presentation-level [UiState] (Loading/Success/Empty/Error), applying
 * an optional emptiness check so an empty successful list renders the
 * dedicated Empty state instead of a blank Success screen.
 */
fun <T> Resource<T>.toUiState(isEmpty: (T) -> Boolean = { false }): UiState<T> = when (this) {
    is Resource.Loading -> UiState.Loading
    is Resource.Error -> UiState.Error(message)
    is Resource.Success -> if (isEmpty(data)) UiState.Empty else UiState.Success(data)
}
