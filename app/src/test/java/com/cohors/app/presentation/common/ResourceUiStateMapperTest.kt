package com.cohors.app.presentation.common

import com.cohors.app.core.util.Resource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourceUiStateMapperTest {

    @Test
    fun `Resource Loading maps to UiState Loading`() {
        assertEquals(UiState.Loading, Resource.Loading.toUiState())
    }

    @Test
    fun `Resource Success with data maps to UiState Success`() {
        val data = listOf("a", "b")
        val uiState = Resource.Success(data).toUiState()
        assertTrue(uiState is UiState.Success)
        assertEquals(data, (uiState as UiState.Success).data)
    }

    @Test
    fun `Resource Success with empty data maps to UiState Empty when isEmpty returns true`() {
        val uiState = Resource.Success(emptyList<String>()).toUiState { it.isEmpty() }
        assertEquals(UiState.Empty, uiState)
    }

    @Test
    fun `Resource Success with non-empty data maps to UiState Success when isEmpty returns true`() {
        val uiState = Resource.Success(listOf("a")).toUiState { it.isEmpty() }
        assertTrue(uiState is UiState.Success)
    }

    @Test
    fun `Resource Error maps to UiState Error with the same message`() {
        val uiState = Resource.Error("something went wrong").toUiState()
        assertTrue(uiState is UiState.Error)
        assertEquals("something went wrong", (uiState as UiState.Error).message)
    }

    @Test
    fun `Resource Success of an empty map maps to UiState Empty`() {
        val uiState = Resource.Success(emptyMap<String, String>()).toUiState { it.isEmpty() }
        assertEquals(UiState.Empty, uiState)
    }
}
