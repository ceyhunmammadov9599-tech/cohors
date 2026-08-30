package com.cohors.app.domain.usecase

import com.cohors.app.core.util.Resource
import com.cohors.app.domain.model.Injury
import com.cohors.app.domain.repository.FootballRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetInjuriesAndSuspensionsUseCaseTest {

    private val repository: FootballRepository = mockk()
    private lateinit var useCase: GetInjuriesAndSuspensionsUseCase

    private fun injury(name: String, type: String?, reason: String?) =
        Injury(playerId = null, playerName = name, playerPhotoUrl = null, teamName = null, leagueName = null, type = type, reason = reason)

    @Before
    fun setUp() {
        useCase = GetInjuriesAndSuspensionsUseCase(repository)
    }

    @Test
    fun `players with a card-related type are classified as suspensions`() = runTest {
        val list = listOf(injury("Player A", type = "Red Card", reason = null))
        every { repository.getInjuries(33, 2024) } returns flowOf(Resource.Success(list))

        val result = (useCase(33, 2024).toList().last() as Resource.Success).data

        assertEquals(1, result.suspensions.size)
        assertEquals(0, result.injuries.size)
    }

    @Test
    fun `players with a suspension keyword in the reason are classified as suspensions`() = runTest {
        val list = listOf(injury("Player B", type = "Disciplinary", reason = "Suspended - accumulation of cards"))
        every { repository.getInjuries(33, 2024) } returns flowOf(Resource.Success(list))

        val result = (useCase(33, 2024).toList().last() as Resource.Success).data

        assertEquals(1, result.suspensions.size)
    }

    @Test
    fun `players with a turkish 'ceza' keyword are classified as suspensions`() = runTest {
        val list = listOf(injury("Player C", type = "Ceza", reason = null))
        every { repository.getInjuries(33, 2024) } returns flowOf(Resource.Success(list))

        val result = (useCase(33, 2024).toList().last() as Resource.Success).data

        assertEquals(1, result.suspensions.size)
    }

    @Test
    fun `players with a regular injury type are classified as injuries`() = runTest {
        val list = listOf(injury("Player D", type = "Muscle Injury", reason = "Hamstring"))
        every { repository.getInjuries(33, 2024) } returns flowOf(Resource.Success(list))

        val result = (useCase(33, 2024).toList().last() as Resource.Success).data

        assertEquals(1, result.injuries.size)
        assertEquals(0, result.suspensions.size)
    }

    @Test
    fun `mixed list is correctly partitioned into injuries and suspensions`() = runTest {
        val list = listOf(
            injury("Injured Player", type = "Knee Injury", reason = null),
            injury("Suspended Player", type = "Suspended", reason = null),
            injury("Another Injured", type = "Muscle Injury", reason = null)
        )
        every { repository.getInjuries(33, 2024) } returns flowOf(Resource.Success(list))

        val result = (useCase(33, 2024).toList().last() as Resource.Success).data

        assertEquals(2, result.injuries.size)
        assertEquals(1, result.suspensions.size)
    }

    @Test
    fun `empty list maps to empty injuries and suspensions`() = runTest {
        every { repository.getInjuries(33, 2024) } returns flowOf(Resource.Success(emptyList()))

        val result = (useCase(33, 2024).toList().last() as Resource.Success).data

        assertEquals(0, result.injuries.size)
        assertEquals(0, result.suspensions.size)
    }

    @Test
    fun `repository errors are propagated unchanged`() = runTest {
        every { repository.getInjuries(33, 2024) } returns flowOf(Resource.Error("injuries fetch failed"))

        val error = useCase(33, 2024).toList().last() as Resource.Error

        assertEquals("injuries fetch failed", error.message)
    }
}
