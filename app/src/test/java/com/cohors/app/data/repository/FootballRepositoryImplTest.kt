package com.cohors.app.data.repository

import com.cohors.app.core.util.Resource
import com.cohors.app.data.remote.api.ApiFootballService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.toList
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Repository tests using a real [MockWebServer] instance — verifies that
 * [FootballRepositoryImpl] correctly parses successful API-Football
 * responses into domain models, and maps HTTP error scenarios (404, 500,
 * timeout) into user-facing [Resource.Error] messages via `resourceFlow`.
 */
class FootballRepositoryImplTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: ApiFootballService
    private lateinit var repository: FootballRepositoryImpl

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(500, TimeUnit.MILLISECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        apiService = retrofit.create(ApiFootballService::class.java)
        repository = FootballRepositoryImpl(apiService)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getLeagues parses a successful response into domain models`() = runTest {
        val json = """
            {
              "get": "leagues",
              "results": 1,
              "response": [
                {
                  "league": { "id": 39, "name": "Premier League", "type": "League", "logo": "https://logo/pl.png" },
                  "country": { "name": "England", "code": "GB", "flag": "https://flag/gb.png" },
                  "seasons": [
                    { "year": 2023, "current": false },
                    { "year": 2024, "current": true }
                  ]
                }
              ]
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val emissions = repository.getLeagues().toList()

        assertEquals(Resource.Loading, emissions.first())
        val success = emissions.last() as Resource.Success
        assertEquals(1, success.data.size)
        val league = success.data.first()
        assertEquals(39, league.id)
        assertEquals("Premier League", league.name)
        assertEquals("England", league.countryName)
        assertEquals(2024, league.currentSeasonYear)
    }

    @Test
    fun `getTeams maps a 404 response to a not-found error message`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("{}"))

        val emissions = repository.getTeams(leagueId = 39, season = 2024).toList()

        assertEquals(Resource.Loading, emissions.first())
        val error = emissions.last() as Resource.Error
        assertEquals("Veri bulunamadı.", error.message)
    }

    @Test
    fun `getSquad maps a 500 response to a server error message`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))

        val emissions = repository.getSquad(teamId = 33).toList()

        assertEquals(Resource.Loading, emissions.first())
        val error = emissions.last() as Resource.Error
        assertEquals("Sunucu hatası. Lütfen daha sonra tekrar deneyin.", error.message)
    }

    @Test
    fun `getInjuries maps an unauthorized 401 response to an auth error message`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody("{}"))

        val emissions = repository.getInjuries(teamId = 33, season = 2024).toList()

        val error = emissions.last() as Resource.Error
        assertEquals("API anahtarı geçersiz veya yetkisiz.", error.message)
    }

    @Test
    fun `getLineup maps a socket timeout to a connectivity error message`() = runTest {
        mockWebServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        val emissions = repository.getLineup(fixtureId = 1).toList()

        assertEquals(Resource.Loading, emissions.first())
        val error = emissions.last() as Resource.Error
        assertEquals("Bağlantı hatası. İnternetinizi kontrol edin.", error.message)
        assertTrue(error.throwable is java.io.IOException)
    }

    @Test
    fun `getTeamInfo returns null gracefully when response list is empty`() = runTest {
        val json = """{ "get": "teams", "results": 0, "response": [] }"""
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val emissions = repository.getTeamInfo(teamId = 9999).toList()

        val success = emissions.last() as Resource.Success
        assertEquals(null, success.data)
    }
}
